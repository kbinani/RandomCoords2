build/libs/RandomCoords2-*.jar: build.gradle $(shell find ./src/main -type f -print)
	./gradlew assemble
	touch $@

.PHONY: clean
clean:
	rm -f build/libs/RandomCoords2-*.jar

.PHONY: run
run: build/libs/RandomCoords2-*.jar
	cd game && java -jar server.jar nogui

.PHONY: vanilla
vanilla:
	(cd vanilla && java -jar server.jar nogui)

.PHONY: dependencyUpdates
dependencyUpdates:
	./gradlew dependencyUpdates -Drevision=release
