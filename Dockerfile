# Run all example modules headlessly — Chrome + Tesseract for OCR profile.
FROM maven:3.9.9-eclipse-temurin-21

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        wget gnupg ca-certificates fonts-liberation \
        tesseract-ocr tesseract-ocr-eng \
    && wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | gpg --dearmor -o /usr/share/keyrings/google-chrome.gpg \
    && echo "deb [arch=amd64 signed-by=/usr/share/keyrings/google-chrome.gpg] http://dl.google.com/linux/chrome/deb/ stable main" \
        > /etc/apt/sources.list.d/google-chrome.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends google-chrome-stable \
    && rm -rf /var/lib/apt/lists/*

ENV CHROME_BIN=/usr/bin/google-chrome
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/5/tessdata

WORKDIR /app
COPY . .

# Framework unit tests + CI-safe demos (mock AI + web patterns). OCR runs in compose profile.
RUN ./mvnw -B -pl framework test \
    && ./mvnw -B -pl examples/ai-healing-demo -am test -Dtest=MockAiHealingDemoTest -Dsurefire.failIfNoSpecifiedTests=false \
    && ./mvnw -B -pl examples/web-patterns-demo -am test -Dsurefire.failIfNoSpecifiedTests=false

CMD ["./mvnw", "-B", "-pl", "examples/web-patterns-demo,examples/ai-healing-demo", "-am", "test", "-Dsurefire.failIfNoSpecifiedTests=false"]