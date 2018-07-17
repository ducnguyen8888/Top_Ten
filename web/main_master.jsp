<%--
  Created by IntelliJ IDEA.
  User: Duc.Nguyen
  Date: 7/16/2018
  Time: 5:06 PM
  To change this template use File | Settings | File Templates.
--%>
<%@ include file="configuration.jsp"
%><jsp:useBean id="topTenUser" class="act.app.topten.TopTenUser" scope="session"
/><%@ page contentType="text/html;charset=UTF-8" language="java"
%><%
    if( topTenUser == null
            || ( topTenUser != null && !topTenUser.isValid()) ) {
%><jsp:forward page="Login.jsp"/><%
    }
%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Title</title>
</head>
<body>

</body>
</html>
