plugins {
	java
	id("org.springframework.boot") version "4.0.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.matjazt"
version = "0.0.1-SNAPSHOT"
description = "Newtork monitoring back end"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.mapstruct:mapstruct:1.5.5.Final")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1")
	implementation("org.springframework.boot:spring-boot-starter-security")
	annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")
	runtimeOnly("org.postgresql:postgresql") 
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
