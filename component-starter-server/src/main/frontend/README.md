mvn package
cd target
unzip component-starter-server-meecrowave-distribution.zip
cd component-starter-server-distribution

export JDK_JAVA_OPTIONS="-Dtalend.component.starter.security.csp=\"default-src 'self' data: unpkg.com; frame-ancestors 'none' ; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'\""                                 talend.component.starter.security.csp>default-src * 'unsafe-eval' 'unsafe-inline'; img-src * data: blob: 'unsafe-inline'; font-src * data: blob: 'unsafe-inline'; frame-ancestors 'none'</talend.component.starter.security.csp>
export JAVA_HOME=/opt/homebrew/opt/openjdk@17

./bin/meecrowave.sh run
