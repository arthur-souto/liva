import json
import re
import subprocess
from pathlib import Path

CONSULTA = """
select json_build_object(
 'colunas', (select json_agg(json_build_object('t', c.table_name, 'c', c.column_name, 'tipo', c.udt_name, 'nulo', c.is_nullable = 'YES')
                             order by c.table_name, c.ordinal_position)
             from information_schema.columns c
             join information_schema.tables t on t.table_name = c.table_name and t.table_schema = c.table_schema
             where c.table_schema = 'public' and t.table_type = 'BASE TABLE' and c.table_name <> 'flyway_schema_history'),
 'restricoes', (select json_agg(json_build_object('t', conrelid::regclass::text, 'tipo', contype,
                 'cols', (select json_agg(a.attname order by k.n) from unnest(conkey) with ordinality k(attnum, n)
                          join pg_attribute a on a.attrelid = conrelid and a.attnum = k.attnum),
                 'ref', case when contype = 'f' then confrelid::regclass::text end))
               from pg_constraint where connamespace = 'public'::regnamespace and contype in ('p', 'f', 'u')),
 'unicos', (select json_agg(json_build_object('t', tablename, 'def', indexdef))
            from pg_indexes where schemaname = 'public' and indexdef like 'CREATE UNIQUE%')
);
"""

AREAS = [
    ("Cadastro, acesso e auditoria",
     "Quem é atendido, quem trabalha na unidade, como cada um entra no sistema e o registro de tudo o que a equipe faz.",
     ["pacientes", "usuarios", "profissionais", "especialidades", "disponibilidades", "cuidadores", "vinculos_de_cuidado",
      "codigos_portal", "links_de_acesso", "auditoria"]),
    ("Fluxo assistencial",
     "Da chegada na unidade à consulta com o especialista: triagem, encaminhamento, agendamento, evolução e remarcação.",
     ["triagens", "retificacoes_triagem", "encaminhamentos", "agendamentos", "evolucoes_clinicas", "remarcacoes"]),
    ("WhatsApp e lembretes",
     "Mensagens enviadas e recebidas, a conversa com o assistente e as rotinas automáticas.",
     ["registros_mensagem", "mensagens_recebidas", "conversas_whatsapp", "tarefas_executadas"]),
    ("Fila de espera e reaproveitamento de vagas",
     "Quem espera por cada especialidade, os horários liberados, as ofertas feitas e o histórico da fila.",
     ["entradas_fila", "vagas_liberadas", "ofertas_de_vaga", "movimentacoes_fila", "confirmacoes_de_presenca"]),
    ("Prevenção de faltas",
     "Por que a pessoa faltou ou cancelou e o resultado das ligações da equipe.",
     ["motivos_de_ausencia", "ligacoes_de_prevencao"]),
]

TIPOS = {"int2": "smallint", "int4": "int", "int8": "bigint", "bool": "boolean"}


def ler_esquema():
    saida = subprocess.run(
        ["docker", "exec", "-i", "liva-postgres", "sh", "-c", 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -tA'],
        input=CONSULTA, capture_output=True, text=True, check=True).stdout
    return json.loads(saida)


def colunas_do_indice(definicao):
    corpo = definicao.split(" USING btree ", 1)[1]
    lista, _, condicao = corpo.partition(" WHERE ")
    nomes = [re.sub(r"[^a-z_]", "", re.sub(r"^lower\(\((\w+)\)::text\)$", r"\1", parte.strip())) for parte in lista.strip()[1:-1].split(",")]
    so_preenchidos = len(nomes) == 1 and condicao.strip() == f"({nomes[0]} IS NOT NULL)"
    return nomes, bool(condicao) and not so_preenchidos


def montar(esquema):
    colunas = {}
    for c in esquema["colunas"]:
        colunas.setdefault(c["t"], []).append(c)
    pk = {r["t"]: r["cols"] for r in esquema["restricoes"] if r["tipo"] == "p"}
    fks = [r for r in esquema["restricoes"] if r["tipo"] == "f"]
    unicas = {}
    for r in esquema["restricoes"]:
        if r["tipo"] == "u" and len(r["cols"]) == 1:
            unicas.setdefault(r["t"], {})[r["cols"][0]] = False
    for u in esquema["unicos"]:
        if u["t"] not in colunas:
            continue
        nomes, parcial = colunas_do_indice(u["def"])
        if len(nomes) == 1 and nomes[0] not in pk.get(u["t"], []):
            atual = unicas.setdefault(u["t"], {}).get(nomes[0])
            unicas[u["t"]][nomes[0]] = parcial if atual is None else atual and parcial
    return colunas, pk, fks, unicas


def atributo(tabela, c, pk, fks, unicas):
    chaves = []
    if c["c"] in pk.get(tabela, []):
        chaves.append("PK")
    if any(f["t"] == tabela and c["c"] in f["cols"] for f in fks):
        chaves.append("FK")
    unica = unicas.get(tabela, {}).get(c["c"])
    if unica is not None and "PK" not in chaves:
        chaves.append("UK")
    notas = []
    if c["nulo"]:
        notas.append("opcional")
    if unica:
        notas.append("única com condição")
    texto = f'        {TIPOS.get(c["tipo"], c["tipo"])} {c["c"]}'
    if chaves:
        texto += " " + ", ".join(chaves)
    if notas:
        texto += f' "{", ".join(notas)}"'
    return texto


def relacao(f, colunas, pk, unicas):
    filha, mae, cols = f["t"], f["ref"], f["cols"]
    nulas = any(c["nulo"] for c in colunas[filha] if c["c"] in cols)
    um_para_um = cols == pk.get(filha) or (len(cols) == 1 and unicas.get(filha, {}).get(cols[0]) is False)
    lado_mae = "|o" if nulas else "||"
    lado_filha = "o|" if um_para_um else "o{"
    return f'    {mae} {lado_mae}--{lado_filha} {filha} : "{", ".join(cols)}"'


def diagrama_da_area(tabelas, colunas, pk, fks, unicas):
    linhas = ["```mermaid", "erDiagram"]
    relacoes = [f for f in fks if f["t"] in tabelas]
    vizinhas = sorted({f["ref"] for f in relacoes if f["ref"] not in tabelas})
    for t in tabelas:
        linhas.append(f"    {t} {{")
        linhas.extend(atributo(t, c, pk, fks, unicas) for c in colunas[t])
        linhas.append("    }")
    for t in vizinhas:
        linhas.append(f"    {t} {{")
        linhas.extend(f'        {TIPOS.get(c["tipo"], c["tipo"])} {c["c"]} PK' for c in colunas[t] if c["c"] in pk.get(t, []))
        linhas.append("    }")
    linhas.extend(relacao(f, colunas, pk, unicas) for f in relacoes)
    linhas.append("```")
    return "\n".join(linhas), vizinhas


def visao_geral(colunas, pk, fks, unicas):
    linhas = ["```mermaid", "erDiagram"]
    vistas = set()
    for f in fks:
        par = (f["ref"], f["t"])
        if par in vistas:
            continue
        vistas.add(par)
        linhas.append(relacao(f, colunas, pk, unicas).rsplit(" : ", 1)[0] + ' : ""')
    soltas = sorted(t for t in colunas if all(t not in (f["t"], f["ref"]) for f in fks))
    for t in soltas:
        linhas.append(f"    {t} {{")
        linhas.extend(f'        {TIPOS.get(c["tipo"], c["tipo"])} {c["c"]} PK' for c in colunas[t] if c["c"] in pk.get(t, []))
        linhas.append("    }")
    linhas.append("```")
    return "\n".join(linhas)


def main():
    esquema = ler_esquema()
    colunas, pk, fks, unicas = montar(esquema)
    nas_areas = {t for _, _, tabelas in AREAS for t in tabelas}
    fora = sorted(set(colunas) - nas_areas)
    if fora:
        raise SystemExit("Tabelas sem área no diagrama: " + ", ".join(fora))

    partes = [
        "# Diagrama relacional do banco",
        "",
        f"Gerado a partir do esquema do PostgreSQL por `scripts/diagrama-do-banco.py`: {len(colunas)} tabelas, "
        f"{sum(len(v) for v in colunas.values())} colunas e {len(fks)} chaves estrangeiras. "
        "A tabela de controle do Flyway não aparece.",
        "",
        "Como ler:",
        "",
        "- `PK` é a chave primária, `FK` é a chave estrangeira e `UK` é um valor que não se repete.",
        "- `||--o{` é um para muitos; `||--o|` é um para no máximo um.",
        "- `|o` do lado de cima quer dizer que a ligação é opcional: a coluna pode ficar vazia.",
        "- \"única com condição\" vale só para parte das linhas, por exemplo apenas as ativas.",
        "",
        "## Visão geral",
        "",
        "Todas as tabelas e como se ligam. As colunas estão nas seções seguintes.",
        "",
        visao_geral(colunas, pk, fks, unicas),
        "",
    ]
    for titulo, descricao, tabelas in AREAS:
        diagrama, vizinhas = diagrama_da_area(tabelas, colunas, pk, fks, unicas)
        partes += [f"## {titulo}", "", descricao, ""]
        if vizinhas:
            partes += ["Tabelas de outras áreas aparecem só com a chave primária: " + ", ".join(f"`{v}`" for v in vizinhas) + ".", ""]
        partes += [diagrama, ""]

    destino = Path(__file__).resolve().parent.parent / "docs" / "DIAGRAMA_DO_BANCO.md"
    destino.write_text("\n".join(partes), encoding="utf-8")
    print(f"{destino}: {len(colunas)} tabelas, {len(fks)} chaves estrangeiras")


if __name__ == "__main__":
    main()
