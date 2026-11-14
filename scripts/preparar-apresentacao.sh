#!/usr/bin/env bash
set -euo pipefail

telefone="${1:-${TELEFONE_APRESENTACAO:-}}"
if [[ -z "$telefone" ]]; then
  echo "Uso: scripts/preparar-apresentacao.sh +55DDDNUMERO   (o WhatsApp que vai receber as mensagens na demonstração)" >&2
  exit 1
fi

digitos="${telefone//[^0-9]/}"
[[ "$digitos" == 55* ]] || digitos="55$digitos"
if [[ ${#digitos} -ne 12 && ${#digitos} -ne 13 ]]; then
  echo "Telefone inválido: use o formato +55DDDNUMERO" >&2
  exit 1
fi
chave="${digitos:2:2}${digitos: -8}"

pasta="$(cd "$(dirname "$0")" && pwd)"
docker exec -i liva-postgres sh -c 'psql -q -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v telefone="$1" -v chave="$2"' _ "+$digitos" "$chave" \
  < "$pasta/apresentacao.sql"
