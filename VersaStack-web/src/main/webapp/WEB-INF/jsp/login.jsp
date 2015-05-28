<!DOCTYPE html>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html lang="en">
    <head>
        <meta http-equiv="content-type" content="text/html; charset=UTF-8">
        <meta charset="utf-8">
        <title>Login Request</title>
        <meta name="viewport" content="initial-scale=1">
        <link href="css/styles.css" rel="stylesheet">
    </head>
    <body>
        <p> The action chosen is: ${action}</p>
        <c:if test="${success == 'true'}">
            Succeeded!<br />
            <a href="test.html"> Click here to continue... </a>
        </c:if>

        <c:if test="${success == 'false'}">
            ${reason}<br />
            <a href="/VersaStack-web"> Click here to return to login... </a>
        </c:if>
    </body>
</html>