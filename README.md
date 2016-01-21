VersaStack FrontEnd MySQL configuration:

1) Run localhost MySQL server, and import localhost.sql, found under the VersaStack-Web/src/main/webapp/tools folder.

In the SQL console - `source [path to localhost.sql file]`

2) Create two users with the following credentials:
          U/N: login_view ; P/W: loginuser
          U/N: front_view ; P/W: frontuser
          
In the SQL console - `CREATE USER 'login_view'@'localhost' IDENTIFIED BY 'loginuser'; CREATE USER 'front_view'@'localhost' IDENTIFIED BY 'frontuser';`
          
3) Give login_view full privileges to Login, and likewise give front_view full privileges to Frontend.

In the SQL console - `GRANT ALL ON login.* TO 'login_view'@'localhost'; GRANT ALL ON frontend.* TO 'front_view'@'localhost';`

Separation of users and restriction of privileges will proceed as development matures.
