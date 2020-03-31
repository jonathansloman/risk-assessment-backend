FROM openjdk:8-jre-alpine

WORKDIR /var/opt/risk-assessment

COPY build/libs/risk-assessment-backend.jar /var/opt/risk-assessment/risk-assessment-backend.jar

EXPOSE 3001

ENTRYPOINT [ "java", "-Dfile.encoding=UTF-8", "-Duser.timezone=UTC", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-cp", "risk-assessment-backend.jar", "uk.co.risk.assessment.PokerServer"]
