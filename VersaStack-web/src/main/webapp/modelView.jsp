<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page errorPage = "/VersaStack-web/errorPage.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<jsp:useBean id="user" class="web.beans.userBeans" scope="session" />
<jsp:setProperty name="user" property="*" />  
<!DOCTYPE html>
<html >    
    <head>   
        <meta charset="UTF-8">
        <title>Model</title>
    </head>

    <body>        

        <div id="main-pane">
            Model<br><br>
            ${user.getTtlModel()}
        </div>        
        <!-- JS -->
        <script>
<<<<<<< HEAD
        </script>        
    </body>
</html>
=======

        </script>        
    </body>
</html>
>>>>>>> 16f82bb2f38eac7e1b2df8be7e4dddd8a8dbbd9e
