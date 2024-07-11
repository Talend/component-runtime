mvn package
cd target
unzip component-starter-server-meecrowave-distribution.zip
cd component-starter-server-distribution

export JDK_JAVA_OPTIONS="-Dtalend.component.starter.security.csp=\"default-src 'self' data: unpkg.com; frame-ancestors 'none' ; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'\""

./bin/meecrowave.sh run
