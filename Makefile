# ======================================================
# 🚀 Plant Shop Play Framework
# ======================================================

run:
	sbt stage
	rm -f target/universal/stage/RUNNING_PID
	target/universal/stage/bin/plant_shop_play_framework -Dhttp.port=4500

compile:
	sbt compile

compile_run: compile run

prod:
	sbt dist
	cd target/universal && unzip -o plant_shop_play_framework-1.0-SNAPSHOT.zip
	target/universal/plant_shop_play_framework-1.0-SNAPSHOT/bin/plant_shop_play_framework -Dhttp.port=4500

# ======================================================
# 🌱 Seed
# ======================================================

seed:
	@sbt "runMain seed.Seed" 2>/dev/null | grep -v "^\[" | grep -v "^$$"

seed-build:
	sbt compile

seed-dev:
	sbt compile
	sbt "runMain seed.Seed"

# ======================================================
# 🗄️ Base de données
# ======================================================

db-create:
	psql -U tilnede0x1182 -d postgres -c "CREATE DATABASE plant_shop_play_framework;"

db-migrate:
	@echo "Les evolutions Play s'appliquent automatiquement au demarrage du serveur."

db-drop:
	psql -U tilnede0x1182 -d postgres -c "DROP DATABASE IF EXISTS plant_shop_play_framework;"

db-seed: seed

db-reset: db-drop db-create db-seed

# ======================================================
# 🧪 Tests
# ======================================================

tests: tests-backend

tests-backend:
	@sbt "Test / runMain test.E2EBackend" 2>/dev/null | grep -v "^\[" | grep -v "^$$"

tests-frontend:
	@sbt "Test / runMain test.E2EFrontend" 2>/dev/null | grep -v "^\[" | grep -v "^$$"

test-build:
	sbt "Test / compile"

test-dev:
	sbt "Test / compile"
	@sbt "Test / runMain test.E2EBackend" 2>/dev/null | grep -v "^\[" | grep -v "^$$"

# ======================================================
# 🛠️ Utilitaires
# ======================================================

tree:
	tree -L 3 -I "target"
