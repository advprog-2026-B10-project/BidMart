import org.gradle.api.plugins.quality.Pmd
import org.gradle.api.plugins.quality.Checkstyle

plugins {
	java
	jacoco
	pmd
	checkstyle
	id("org.springframework.boot") version "3.5.11"
	id("io.spring.dependency-management") version "1.1.7"
	id("net.serenity-bdd.serenity-gradle-plugin") version "4.1.3"
}

group = "id.ac.ui.cs.advprog"
version = "0.0.1-SNAPSHOT"
description = "bidmart"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.postgresql:postgresql")
	implementation("org.springframework.boot:spring-boot-starter-amqp")
	implementation ("org.springframework.boot:spring-boot-starter-security")
	implementation ("io.jsonwebtoken:jjwt-api:0.12.5")
	implementation ("org.springframework.boot:spring-boot-starter-validation")
	implementation ("org.springframework.boot:spring-boot-starter-mail")
    runtimeOnly ("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly ("io.jsonwebtoken:jjwt-jackson:0.12.5")
	compileOnly("org.projectlombok:lombok")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("com.h2database:h2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	runtimeOnly("org.postgresql:postgresql")

	testImplementation("net.serenity-bdd:serenity-core:4.1.3")
	testImplementation("net.serenity-bdd:serenity-junit5:4.1.3")
	testImplementation("net.serenity-bdd:serenity-spring:4.1.3")
	testImplementation("net.serenity-bdd:serenity-screenplay:4.1.3")
	testImplementation("net.serenity-bdd:serenity-screenplay-webdriver:4.1.3")
}

tasks.withType<Test> {
	useJUnitPlatform()
	finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

tasks.jacocoTestReport {
	dependsOn(tasks.test) // tests are required to run before generating the report
	reports {
		xml.required.set(false)
		csv.required.set(false)
		html.outputLocation.set(layout.buildDirectory.dir("jacocoHtml"))
	}
}

pmd {
	isIgnoreFailures = true
	toolVersion = "6.55.0"
}

tasks.withType<Pmd> {
	reports {
		xml.required.set(false)
		html.required.set(true)
	}
}

checkstyle {
	isIgnoreFailures = true
	toolVersion = "10.15.0"
}

tasks.withType<Checkstyle> {
	reports {
		xml.required.set(false)
		html.required.set(true)
	}
}
