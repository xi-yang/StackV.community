<!--
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Antonio Heard 2016

 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and/or hardware specification (the “Work”) to deal in the 
 * Work without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Work, and to permit persons to whom the Work is furnished to do so, 
 * subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Work.

 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS  
 * IN THE WORK.
 !-->

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page errorPage = "/StackV-web/errorPage.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<link rel="stylesheet" href="/StackV-web/css/tagPanel.css">       
<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/font-awesome/4.6.1/css/font-awesome.min.css">

<div class="closed" id="tagPanel" data-toggle="popover">
    <div id="tagPanel-tab">
        Tags
    </div>
    <div id ="tagPanel-contents">
        <div id="tagPanel-colorPanel">
            <div id="tagPanel-colorPanelTitle"> Filter Colors</div>
            <div id="tagPanelColorSelectionTab">
                <span class="filteredColorBox" id="boxRed"></span>
                <span class="filteredColorBox" id="boxOrange"></span>
                <span class="filteredColorBox" id="boxYellow"></span>
                <span class="filteredColorBox" id="boxGreen"></span>
                <span class="filteredColorBox" id="boxBlue"></span>
                <span class="filteredColorBox" id="boxPurple"></span>
            </div>
        </div>
        <div id="tagPanel-labelPanel">
            <div id="tagPanel-labelPanelTitle">Labels</div>
            <button id="ClearAllTagsButton">Clear All</button>
            <div id="labelList-container">
                <ul class="tagPanel-labelList" id="labelList1">
                </ul>
            </div>
        </div>
    </div>
</div>