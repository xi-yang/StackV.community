<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page errorPage = "/vxstack-web/errorPage.jsp" %>
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

        </script>        
    </body>
</html>
