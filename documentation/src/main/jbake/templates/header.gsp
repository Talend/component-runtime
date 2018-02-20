<!DOCTYPE html>
<%/*
  Copyright (C) 2006-2018 Talend Inc. - www.talend.com
   Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/%>
<html lang="en">
  <head>
    <script async src="https://www.googletagmanager.com/gtag/js?id=GTM-PSBN"></script>
    <script>
      window.dataLayer = window.dataLayer || [];
      function gtag(){ dataLayer.push(arguments); }
      gtag('js', new Date());
      gtag('config', 'GTM-PSBN');
    </script>

    <meta charset="utf-8">
    <title>Talend Component Documentation</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="date" content="<%= new Date().format('YYYY-MM-dd HH:mm:ss')%>" scheme="YYYY-MM-dd HH:mm:ss">
    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <link rel="stylesheet" href="css/bootstrap.css" media="screen" />
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/ink/3.1.0/css/font-awesome.min.css" integrity="sha256-MUe1D3T3ocGLZQ2h8wgpcIz5BoTu1OZo8q0BjK5u/o0=" crossorigin="anonymous" />
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/github-fork-ribbon-css/0.2.0/gh-fork-ribbon.min.css" />
    <!--[if lt IE 9]>
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/github-fork-ribbon-css/0.2.0/gh-fork-ribbon.ie.min.css" />
    <![endif]-->
    <link rel="stylesheet" href="css/documentation.css">
    <link rel="shortcut icon" href="images/favicon_0.ico" />
    <style>
    body{padding-top:50px}body>.navbar{-webkit-transition:background-color .3s ease-in;transition:background-color .3s ease-in}@media (min-width:768px){body>.navbar-transparent{background-color:transparent}body>.navbar-transparent .navbar-nav>.open>a{background-color:transparent!important}}#home{padding-top:0}#home .navbar-brand{padding:13.5px 15px 12.5px}#home .navbar-brand>img{display:inline;margin:0 10px;height:100%}#banner{min-height:300px;border-bottom:none}.table-of-contents{margin-top:1em}.page-header h1{font-size:4em}.bs-docs-section{margin-top:6em}.bs-docs-section h1{padding-top:100px}.bs-component{position:relative}.bs-component .modal{position:relative;top:auto;right:auto;left:auto;bottom:auto;z-index:1;display:block}.bs-component .modal-dialog{width:90%}.bs-component .popover{position:relative;display:inline-block;width:220px;margin:20px}#source-button{position:absolute;top:0;right:0;z-index:100;font-weight:700}.progress{margin-bottom:10px}footer{margin:5em 0}footer li{float:left;margin-right:1.5em;margin-bottom:1.5em}footer p{clear:left;margin-bottom:0}.splash{padding:9em 0 2em;background-color:#141d27;background-image:url(../img/bg.jpg);background-size:cover;background-attachment:fixed;color:#fff;text-align:center}.splash .logo{width:160px}.splash h1{font-size:3em}.splash #social{margin:2em 0}.splash .alert{margin:2em 0}.section-tout{padding:4em 0 3em;border-bottom:1px solid rgba(0,0,0,.05);background-color:#eaf1f1}.section-tout .fa{margin-right:.5em}.section-tout p{margin-bottom:3em}.section-preview{padding:4em 0 4em}.section-preview .preview{margin-bottom:4em;background-color:#eaf1f1}.section-preview .preview .image{position:relative}.section-preview .preview .image:before{box-shadow:inset 0 0 0 1px rgba(0,0,0,.1);position:absolute;top:0;left:0;width:100%;height:100%;content:"";pointer-events:none}.section-preview .preview .options{padding:1em 2em 2em;border:1px solid rgba(0,0,0,.05);border-top:none;text-align:center}.section-preview .preview .options p{margin-bottom:2em}.section-preview .dropdown-menu{text-align:left}.section-preview .lead{margin-bottom:2em}@media (max-width:767px){.section-preview .image img{width:100%}}.sponsor #carbonads{max-width:240px;margin:0 auto}.sponsor .carbon-text{display:block;margin-top:1em;font-size:12px}.sponsor .carbon-poweredby{float:right;margin-top:1em;font-size:10px}@media (max-width:767px){.splash{padding-top:4em}.splash .logo{width:100px}.splash h1{font-size:2em}#banner{margin-bottom:2em;text-align:center}}
    </style>
  </head>
  <body>
    <noscript><iframe src="https://www.googletagmanager.com/ns.html?id=GTM-PSBN"
    height="0" width="0" style="display:none;visibility:hidden"></iframe></noscript>

    <a class="github-fork-ribbon" target="_blank"
        href="https://github.com/talend/component-runtime" data-ribbon="Fork me on GitHub"
        title="Fork me on GitHub">Fork me on GitHub</a>
    <div class="col-sm-offset-1 col-sm-10">
