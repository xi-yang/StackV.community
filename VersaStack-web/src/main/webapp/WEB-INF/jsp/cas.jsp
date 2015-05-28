<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="content-type" content="text/html; charset=UTF-8">
        <meta charset="utf-8">
        <title>CAS Login Request</title>
        <meta name="viewport" content="initial-scale=1">
        <link href="css/styles.css" rel="stylesheet">
    </head>
    <body>
        <p> CAS login succeeded </p>
        <a href="test.html"> Click here to continue... </a>
        <br /><br />
        <!-- TODO get permission to use attributes -->
        <p>Server returned attributes: </p>
        <c:forEach var="attribute" items="${attributesMap}">
            Key: <c:out value="${attribute.key}"/>
            Value: <c:out value="${attribute.value}"/>
        </c:forEach>
    </body>
</html>
