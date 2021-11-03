build/libs/RandomCoords2-*.jar: build.gradle $(shell find ./src/main -type f -print)
	./gradlew assemble
	touch $@

.PHONY: clean
clean:
	rm -f build/libs/RandomCoords2-*.jar

.PHONY: run
run: build/libs/RandomCoords2-*.jar
	rm -rf build
	./gradlew assemble
	cd game/plugins && ln -sf ../../build/libs/RandomCoords2-*.jar ./
	cd game && JAVA_HOME=/Library/Java/JavaVirtualMachines/adoptopenjdk-16.jdk/Contents/Home/ /Library/Java/JavaVirtualMachines/adoptopenjdk-16.jdk/Contents/Home/bin/java --illegal-access=permit -jar server.jar nogui

.PHONY: vanilla
vanilla:
	(cd vanilla && java -jar server.jar nogui)

.PHONY: dependencyUpdates
dependencyUpdates:
	./gradlew dependencyUpdates -Drevision=release
