FROM openjdk:8-jre-alpine

WORKDIR /var/opt/risk-assessment

COPY build/libs/risk-assessment-backend-all.jar /var/opt/risk-assessment/risk-assessment-backend-all.jar
COPY www /var/opt/risk-assessment/www

EXPOSE 3001 80

ENTRYPOINT [ "java", "-Dfile.encoding=UTF-8", "-Duser.timezone=UTC", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-jar", "risk-assessment-backend-all.jar"]
