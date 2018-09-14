<!--
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Antonio Heard 2016

 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and/or hardware specification (the ?Work?) to deal in the 
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
<!DOCTYPE html>
<html>

<head>
    <meta charset="UTF-8">
    <title>Manifest Portal</title>
    <link rel="stylesheet" href="/StackV-web/css/style.css">
    <style>
        .manifest-list {
            padding-left: 1.5em;
        }
    </style>
</head>

<body>
    <!-- MAIN PANEL -->
    <div id="main-pane">
        <table class="management-table">
            <thead>
                <tr>
                    <th colspan="2"> Manifest Portal </th>
                </tr>
            </thead>
            <tbody id="manifest_table_body"></tbody>
        </table>
    </div>
</body>

<<!-- CORE JS PACKAGE-->
    <script src="https://k152.maxgigapop.net:8543/auth/js/keycloak.js"></script>
    <script src="https://code.jquery.com/jquery-2.2.4.min.js" crossorigin="anonymous"></script>
    <script type="text/javascript" src="https://cdnjs.cloudflare.com/ajax/libs/izimodal/1.5.1/js/iziModal.min.js"></script>
    <script src="/StackV-web/js/iziToast.min.js"></script>

    <script src="/StackV-web/js/jquery-ui.min.js"></script>
    <script src="/StackV-web/js/greensock/TweenLite.min.js"></script>
    <script src="/StackV-web/js/greensock/plugins/CSSPlugin.min.js"></script>
    <!-- -->

    <script src="/StackV-web/js/bundles/StackV-main.bundle.js"></script>
    <!---->

    <script src="/StackV-web/js/manifest.js"></script>

</html>