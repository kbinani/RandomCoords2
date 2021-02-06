VERSION := 0.1.0

build/libs/RandomCoords2-$(VERSION).jar: build.gradle $(shell find ./src/main -type f -print)
	./gradlew assemble
	touch $@

.PHONY: clean
clean:
	rm -f build/libs/RandomCoords2-$(VERSION).jar

.PHONY: run
run: build/libs/RandomCoords2-$(VERSION).jar
	cd game && java -jar server.jar nogui

.PHONY: vanilla
vanilla:
	(cd vanilla && java -jar server.jar nogui)

.PHONY: dependencyUpdates
dependencyUpdates:
	gradle dependencyUpdates -Drevision=release
