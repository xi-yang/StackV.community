VersaStack FrontEnd MySQL configuration:

1) Run localhost MySQL server, and import localhost.sql, found under the VersaStack-Web module.
2) Create two users with the following credentials:
          U/N: login_view ; P/W: loginuser
          U/N: front_view ; P/W: frontuser
          
3) Give login_view full privileges to Login, and likewise give front_view full privileges to Frontend.

Separation of users and restriction of privileges will proceed as development matures.
