#!/bin/sh
# Punto de entrada del build; requiere Maven instalado en el entorno.
set -eu
exec mvn "$@"
