CREATE USER 'login_view'@'%' IDENTIFIED BY 'loginuser';
CREATE USER 'login_view'@'localhost' IDENTIFIED BY 'loginuser'; 
 
CREATE USER 'front_view'@'%' IDENTIFIED BY 'frontuser';
CREATE USER 'front_view'@'localhost' IDENTIFIED BY 'frontuser';


GRANT ALL ON login.* TO 'login_view'@'localhost'; 
GRANT ALL ON frontend.* TO 'front_view'@'localhost';

GRANT ALL ON login.* TO 'login_view'@'%'; 
GRANT ALL ON frontend.* TO 'front_view'@'%';