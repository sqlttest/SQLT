SQLT is a generated-based fuzzer focus on test SQL-datatype related bugs based on SQLancer.

to run the fuzz
```
cd SQLT
mvn package -DskipTests
cd target
// openGauss test
java -jar sqlancer-2.0.0.jar --num-threads 20 --host databaseip --port yourport --username gaussdb --password yourpassword  opengauss  --oracle NOREC

// MySQL test
java -jar sqlancer-2.0.0.jar --num-threads 20 --host 211.81.52.44 --port 3305 --username root --password MySQLTesting  mysql  --oracle NOREC

// Sqlite test
java -jar sqlancer-2.0.0.jar --num-threads 20  sqlite3  --oracle NOREC
```