#!/bin/bash
mysql -uroot -p"$MYSQL_ROOT_PASSWORD" <<EOF
CREATE USER 'tecnofactory_app'@'%' IDENTIFIED BY '${APP_DB_PASSWORD}';
GRANT SELECT, INSERT, UPDATE, DELETE ON tecnofactory.* TO 'tecnofactory_app'@'%';
FLUSH PRIVILEGES;
EOF
