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
<html >    
    <head>   
        <meta charset="UTF-8">
        <title>Manifest Portal</title>
        <link rel="stylesheet" href="/StackV-web/css/style.css">        
        <style>
            .manifest-list{
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

    <!-- CORE JS PACKAGE-->
    <script src="https://k152.maxgigapop.net:8543/auth/js/keycloak.js"></script>

    <script src="https://code.jquery.com/jquery-2.2.4.min.js"
            integrity="sha256-BbhdlvQf/xTY9gja0Dq3HiwQF8LaCRTXxZKRutelT44="
            crossorigin="anonymous">
    </script>   
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js" 
            integrity="sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa" 
            crossorigin="anonymous">
    </script>
    <script src="https://code.jquery.com/ui/1.12.1/jquery-ui.js"
            integrity="sha256-T0Vest3yCU7pafRw9r+settMBX6JkKN06dqBnpQ8d30="
            crossorigin="anonymous">
    </script>

    <script src="https://unpkg.com/sweetalert/dist/sweetalert.min.js"></script>                

    <script src="/StackV-web/js/greensock/TweenLite.min.js"></script>
    <script src="/StackV-web/js/greensock/plugins/CSSPlugin.min.js"></script>
    <script src="/StackV-web/js/mousetrap.js"></script>
    <script src="/StackV-web/js/mousetrap-dict.js"></script>

    <script src="/StackV-web/js/nexus.js"></script>
    <!---->

    <script src="/StackV-web/js/manifest.js"></script>
</html>