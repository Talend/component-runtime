/**
 *  Copyright (C) 2006-2022 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

// not the most sexy way to get a theme
// but we can invest on it later,
// for now it looks asciidoctor+talend and it is enough

def asciidocCss = new File(project.basedir, 'src/main/frontend/node_modules/@asciidoctor/core/dist/css/asciidoctor.css')

// apply talend colors
def customized = asciidocCss.text
    .replace('color:rgba(0,0,0,.85);', 'color:rgb(26, 73, 110);')
    .replace('color:rgba(0,0,0,.8);', 'color:rgb(26, 73, 110);')
    .replace('color:#ba3925;', 'color:rgb(83, 83, 83);')
    .replace('color:#7a2518;', 'color:rgb(83, 83, 83);')

// write it in the right location
log.info('Creating talend.css')
def css = new File(project.build.outputDirectory, 'resources/html/talend.css')
css.parentFile.mkdirs()
def cssOs = css.newOutputStream()
cssOs << customized
cssOs << """
body {height: 100vh;color:rgb(83, 83, 83) !important;}
table tr th, table tr td {color:rgb(83, 83, 83) !important;}
#content{margin-bottom:80px;}
#footer{position:fixed;bottom: 0;}
#footer-text:before{margin-top:6px;height:34px;width:128px;float:left;content:"";margin-right:10px;background:url("data:image/svg+xml;base64,PHN2ZyBjbGFzcz0idGFsZW5kLWxvZ28iIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgdmlld0JveD0iMCAwIDEzNCAzMiIgZmlsbD0icmdiKDI1NSwyNTUsMjU1KSI+CiAgPGcgY2xhc3M9InRhbGVuZC1sb2dvIHZhbHVlNSI+CiAgICA8cG9seWdvbiBjbGFzcz0idGFsZW5kLWxvZ28gbGluZTUiIHBvaW50cz0iMTYuNjUgMzAuNzQzIDE2LjM4NyAzMC41MzggMjcuNzYzIDE2LjQ0OCAyOC4wMjYgMTYuNjU0IDE2LjY1IDMwLjc0MyIvPgogICAgPHBhdGggY2xhc3M9InRhbGVuZC1sb2dvIGNpcmNsZTUiIGQ9Ik0xNS41NDE0OSwzMS41OTk1OWExLjQwMDQyLDEuNDAwNDIsMCwwLDAsMS45NTkxMS4wMDE2MiwxLjM0NjkxLDEuMzQ2OTEsMCwwLDAsLjAwMDg0LTEuOTI4NzksMS40MDU0MSwxLjQwNTQxLDAsMCwwLTEuOTYxOTMuMDAxMTJBMS4zNDU3NiwxLjM0NTc2LDAsMCwwLDE1LjU0MTQ5LDMxLjU5OTU5WiIgdHJhbnNmb3JtPSJ0cmFuc2xhdGUoMCAwKSIvPgogIDwvZz4KICA8ZyBjbGFzcz0idGFsZW5kLWxvZ28gdmFsdWU0Ij4KICAgIDxwb2x5Z29uIGNsYXNzPSJ0YWxlbmQtbG9nbyBsaW5lNCIgcG9pbnRzPSIxOC44NzEgMjAuODI3IDE4LjczIDIwLjUyOCAyNy44MjggMTYuMzkxIDI3Ljk2OSAxNi42OTEgMTguODcxIDIwLjgyNyIvPgogICAgPHBhdGggY2xhc3M9InRhbGVuZC1sb2dvIGNpcmNsZTQiIGQ9Ik0xNy44MTU2OCwxOS43MTU5MmExLjQwMiwxLjQwMiwwLDAsMSwxLjk1OTI4LS4wMDA3OSwxLjM0Mjc3LDEuMzQyNzcsMCwwLDEsMCwxLjkyOCwxLjQwMDg1LDEuNDAwODUsMCwwLDEtMS45NjA5My0uMDAxOTVBMS4zNDcyLDEuMzQ3MiwwLDAsMSwxNy44MTU2OCwxOS43MTU5MloiIHRyYW5zZm9ybT0idHJhbnNsYXRlKDAgMCkiLz4KICA8L2c+CiAgPGcgY2xhc3M9InRhbGVuZC1sb2dvIHZhbHVlMyI+CiAgICA8cmVjdCBjbGFzcz0idGFsZW5kLWxvZ28gbGluZTMiIHg9IjExLjUzNzgyIiB5PSIxNi4zNjg3OCIgd2lkdGg9IjE2LjMyODc0IiBoZWlnaHQ9IjAuMzMwMDYiLz4KICAgIDxwYXRoIGNsYXNzPSJ0YWxlbmQtbG9nbyBjaXJjbGUzIiBkPSJNMTAuNTU0NTcsMTUuNTcyMzJhMS40MDA5MiwxLjQwMDkyLDAsMCwxLDEuOTYwMjUtLjAwMTI5LDEuMzQ2NTcsMS4zNDY1NywwLDAsMSwwLDEuOTI3NDcsMS40MDQxMiwxLjQwNDEyLDAsMCwxLTEuOTYxOS0uMDAwM0ExLjM0NSwxLjM0NSwwLDAsMSwxMC41NTQ1NywxNS41NzIzMloiIHRyYW5zZm9ybT0idHJhbnNsYXRlKDAgMCkiLz4KICA8L2c+CiAgPGcgY2xhc3M9InRhbGVuZC1sb2dvIHZhbHVlMiI+CiAgICA8cG9seWdvbiBjbGFzcz0idGFsZW5kLWxvZ28gbGluZTIiIHBvaW50cz0iMjcuODI4IDE2LjY4NCAxLjMwNSA0LjYzNSAxLjQ0NiA0LjMzNSAyNy45NjkgMTYuMzg1IDI3LjgyOCAxNi42ODQiLz4KICAgIDxwYXRoIGNsYXNzPSJ0YWxlbmQtbG9nbyBjaXJjbGUyIiBkPSJNLjQwNTgzLDMuNTI0OTFhMS40MDQ0NiwxLjQwNDQ2LDAsMCwxLDEuOTYwNDItLjAwMTQ1LDEuMzQ1NzksMS4zNDU3OSwwLDAsMSwwLDEuOTI3NUExLjQwMSwxLjQwMSwwLDAsMSwuNDA0MTgsNS40NDkzNCwxLjM0NTUzLDEuMzQ1NTMsMCwwLDEsLjQwNTgzLDMuNTI0OTFaIiB0cmFuc2Zvcm09InRyYW5zbGF0ZSgwIDApIi8+CiAgPC9nPgogIDxnIGNsYXNzPSJ0YWxlbmQtbG9nbyB2YWx1ZTEiPgogICAgPHBvbHlnb24gY2xhc3M9InRhbGVuZC1sb2dvIGxpbmUxIiBwb2ludHM9IjI3Ljc2MyAxNi42MjQgMTkuNTU5IDYuNDYxIDE5LjgyMiA2LjI1NiAyOC4wMjYgMTYuNDE5IDI3Ljc2MyAxNi42MjQiLz4KICAgIDxwYXRoIGNsYXNzPSJ0YWxlbmQtbG9nbyBjaXJjbGUxIiBkPSJNMTguNzEzLDUuMzk5MzdhMS40MDA0OSwxLjQwMDQ5LDAsMCwxLDEuOTU5MTQtLjAwMTYyQTEuMzQ2ODgsMS4zNDY4OCwwLDAsMSwyMC42NzMsNy4zMjY1NGExLjQwNTM3LDEuNDA1MzcsMCwwLDEtMS45NjE5LS4wMDExMkExLjM0NTc2LDEuMzQ1NzYsMCwwLDEsMTguNzEzLDUuMzk5MzdaIiB0cmFuc2Zvcm09InRyYW5zbGF0ZSgwIDApIi8+CiAgPC9nPgogIDxwYXRoIGNsYXNzPSJ0YWxlbmQtbG9nbyB0ZXh0LXRhbGVuZCIgZD0iTTU4Ljc5OTg1LDE5LjA0MjlhNi42NzEsNi42NzEsMCwwLDAtMy4wMjcuNjI3NDEsMi40OTM2OSwyLjQ5MzY5LDAsMCwwLTEuMzE2NywyLjM1MDUzLDIuNzY2NSwyLjc2NjUsMCwwLDAsMy4wNzAwNywyLjg2MTg5LDYuMzE5NzgsNi4zMTk3OCwwLDAsMCw0LjEwMzUxLTEuNzYzODlWMTguOTI1NzdabTMuMTA5NDIsNi44MTg4OEE3Ljg0NDA2LDcuODQ0MDYsMCwwLDEsNTYuNDA5NzMsMjguMTM1Yy0zLjU0NjE0LDAtNi4xMzg4NC0yLjMxMjgxLTYuMTM4ODQtNS44NzgxOGE1LjI1NzU1LDUuMjU3NTUsMCwwLDEsMy4xNDg2LTUuMDE2MjcsMTEuNjg4MzksMTEuNjg4MzksMCwwLDEsNC44MjI4MS0uOTc5NzFsMy4zODc0Ny0uMTE3NDd2LS43ODIxYzAtMi40MzE0My0xLjU1NDczLTMuNDQ5MzMtMy43ODYxNS0zLjQ0OTMzYTEwLjE5MTU1LDEwLjE5MTU1LDAsMCwwLTUuMDIxNSwxLjM3MTk1bC0xLjQzNjM1LTIuNzA0NDJhMTQuMzEwMzYsMTQuMzEwMzYsMCwwLDEsNy4wOTU4Ni0xLjg0MjU4YzQuNjY0MTUsMCw3LjI5NTIyLDIuMTE3ODEsNy4yOTUyMiw2LjYyNDM4djcuMTI5MjZhMTAuNTY0ODQsMTAuNTY0ODQsMCwwLDAsLjk1OSw1LjI1MDYxSDYyLjU4NTY3Wk0zOC43NTc0MSw5LjIwNzZWNC45MzgxNUw0Mi44OTk2LDMuNjgxODlWOS4yMDc2aDUuNjYxTDQ3LjUyNywxMi40MjA2N0g0Mi44OTk2VjIyLjQ1Mjc2YzAsMS42ODI1LjYzODY5LDIuMzQ5NzEsMS44NzUzOCwyLjM0OTcxYTUuMzU4OTIsNS4zNTg5MiwwLDAsMCwyLjg4MzItLjg5Njg2QTUuNjk4ODcsNS42OTg4NywwLDAsMCw0OC43NjYsMjYuODQ4ODgsMTAuMTYyMjMsMTAuMTYyMjMsMCwwLDEsNDMuNzc5MjQsMjguMTM1Yy0zLjE0OTQxLDAtNS4wMjE4My0xLjc2NDcyLTUuMDIxODMtNC43ODFWOS4yMDc2Wm01MS4xNSw3LjM2NjgxYy0uMDc5ODQtMi4zNTIxMi0xLjIzNDIxLTQuNjYyNTItMy45MDU0Ny00LjY2MjUyLTIuNTEyNTYsMC0zLjk4NTQ3LDIuMTU1MzQtNC4wMjUyOSw0LjY2MjUyWm0tNy44OTA5NCwyLjg5ODY2YTUuMjYyNDcsNS4yNjI0NywwLDAsMCw1LjYxODM5LDUuNDA5NjcsOS41NDk2OSw5LjU0OTY5LDAsMCwwLDQuNzQ0MjktMS4yNTU2bDEuMjM1NzIsMi43ODQ1M0ExMi4xMjUzOCwxMi4xMjUzOCwwLDAsMSw4Ny4xOTYzMSwyOC4xMzVjLTYuNTc0NzUsMC05LjQwNDE3LTQuNTg0ODYtOS40MDQxNy05LjcxOTI5LDAtNS42MDMsMy4xNDg0My05LjY3ODgyLDguMjQ5My05LjY3ODgyLDUuMTQxNjUsMCw4LjA5MjI0LDMuNzYyOTUsOC4wOTIyNCw5Ljc1ODE0di45NzgwOVptLTMuOTU2LDcuNjExNzdhMTEuMjQ0NSwxMS4yNDQ1LDAsMCwxLTQuNTk3MjQuOTMwNTNjLTIuOTA4NjIsMC00LjM0NTMxLTEuNDg4NzktNC4zNDUzMS00LjE5MTc5VjEuNDkwOUw3My4yNjI5MiwwVjIyLjY4Njc3YzAsMS4zNzI3Ny40NzgxOCwxLjc2MzczLDEuMzk1NjksMS43NjM3M2ExMC4zOTEsMTAuMzkxLDAsMCwwLDEuNDAxLS4yNjQ2MUE3LjY1MjQ4LDcuNjUyNDgsMCwwLDAsNzguMDYwNDksMjcuMDg0ODRaTTk2LjkwNTA4LDkuMjA3NmgzLjgwNDQ4bC0uMDUzNDUsMi4zNjc3NmExMS4xNjUsMTEuMTY1LDAsMCwxLDYuNjg5ODMtMi44MzY5MWMyLjk4ODc2LDAsNS44OTc1NSwxLjU2NzYxLDUuODk3NTUsNi42NjI0VjI3Ljc0MDZoLTQuMTQyNjZWMTYuNjkzMTljMC0yLjQzMDQ3LS41NTk0OS00LjM4OTA3LTMuMDY5Ni00LjM4OTA3YTguMjE4NzcsOC4yMTg3NywwLDAsMC00Ljk4MjMyLDIuMjMzNjlWMjcuNzQwNkg5Ni45MDUwOFptMzYuMTM0MywxMy4yODI4OFYuMDQwOGwtLjAwMzI5LjAxMTQyVjBsLTQuMTQ1MTUsMS40OTA5VjkuMDEyNzdhMTMuMTE0NTgsMTMuMTE0NTgsMCwwLDAtMi43NDk3NS0uMjc0MzFjLTUuNzI1MDgsMC0xMC4wMTI1MSwzLjkwODMtMTAuMTUxMiw5LjYzMDYzbC0uMDAzLjAzNTc4Yy0uMDAwNjcuMDI5LS4wMDMyOS4wNjUtLjAwMzI5LjEwNzgzLS4wMDE2NS4wNDYyNC0uMDA3MjIuMDkxMjMtLjAwNzIyLjEzODI2LDAsLjAyNzEuMDAzNjMuMDU1LjAwMzYzLjA4MjY4LS4wMDk1LDEuMzU3NjIuMjMzNDMsNS43MDI2OSwzLjU2MjUyLDguMDY1LjAyLjAxNDQ5LjA0LjAyODY4LjA2MDMzLjA0MjM4LjEyOTQ5LjA5MTA2LjI2NTIzLjE3NzYuNDA0NTkuMjYyMi4wMzQwOC4wMTk2Ny4wNjUyMy4wNDA0Ni4wOTYzOS4wNTkxNS4xNjEuMDkzLjMyNDU4LjE4MzkxLjQ5NzY4LjI2NzM4LDAsMCwuMDI2MjIuMDExNDUuMDYzOTIuMDMuMDg5MjEuMDQzODMuMTgyLjA4Njg3LjI3NTEuMTI3NjMuMDIxNjIuMDA4NzEuMDQyOTQuMDE4MDUuMDY1OTEuMDI2NDQuMTYyMjkuMDY4ODIuMzI3NS4xMzE2Ni40OTYuMTg1YS4xMzIzOC4xMzIzOCwwLDAsMSwuMDIzNi4wMDczOWMuMTUxMTUuMDQ4NTIuMzA1ODguMDkxMjMuNDYzOTEuMTI5NzUuMDYyNjUuMDE3ODkuMTI2OTEuMDI4MTkuMTg4ODUuMDQxNTkuMTA4MjEuMDIzMzcuMjE1NC4wNDUyOC4zMjM5NS4wNjM0Ny4xMTE0Ni4wMTY0Ny4yMi4wMzEuMzI5ODIuMDQxOTIuMDkyOC4wMDk4NC4xODUyNi4wMjA3OS4yNzk2Ni4wMjg1Mi4wNDc4OC4wMDMyMy4wOTc3LjAwNDUyLjE0NDkzLjAwNTY0LjEzODY2LjAwODA1LjI3NDA5LjAxNzg5LjQxNTcuMDE3ODlhNy44NDA0OSw3Ljg0MDQ5LDAsMCwwLDUuNDk5MjEtMi4yNzMxN2wuNjc3NywxLjg3OTMxSDEzNEExMC41NDgsMTAuNTQ4LDAsMCwxLDEzMy4wMzkzOCwyMi40OTA0OFpNMTI0LjA3MywyNC44MjAxOWEzLjc1OTEyLDMuNzU5MTIsMCwwLDEtMS4yNzQwNi0uNDYxNTVjLS4wMjQ1OC0uMDEzNTMtLjA1MTgtLjAyODg1LS4wNzYtLjA0My0uMDI5NTEtLjAxNzIzLS4wNjA2Ny0uMDMyODctLjA4NzU2LS4wNTIyMmE0Ljg1MDc0LDQuODUwNzQsMCwwLDEtLjUwOTUtLjM1NDIyYy0uMTAzNjEtLjA5ODE2LS4xOTI0NC0uMTk3NzQtLjI4NDIzLS4yOTkxMy0uMDM5MzUtLjAzODgxLS4wODAzNC0uMDc5MjgtLjExOC0uMTIyLS4wMjAzMS0uMDI0MTYtLjA0MTYzLS4wNDkzMS0uMDYyMjgtLjA3MjE4YTUuMjYyMTcsNS4yNjIxNywwLDAsMS0uNjI5MTktLjkwMjg0Yy0uMDQyOTQtLjA3OTExLS4wNzg2Ni0uMTU5MzgtLjExOC0uMjM4Mi0uMDIwMzQtLjA0Mi0uMDQzOTUtLjA4MjgxLS4wNjQyNi0uMTI2MThhNy45ODA4OCw3Ljk4MDg4LDAsMCwxLS41ODItNC4yMjQsOC4xMjEsOC4xMjEsMCwwLDEsLjEyNzIxLS45NDA4NmMuMDA0NTctLjAyNDM2LjAwNjU1LS4wNTIyMi4wMTA4MS0uMDc1YTcuMzkyOTMsNy4zOTI5MywwLDAsMSwuMzAwNjUtMS4wMzg1Miw1Ljc4MDIzLDUuNzgwMjMsMCwwLDEsNS42NzMtMy43MjMsOS4zMzU2LDkuMzM1NiwwLDAsMSwyLjUwMDkxLjMxMDA5VjIzLjExODg0YTYuMTI2NDksNi4xMjY0OSwwLDAsMS00LjA5MywxLjc2Mzg5QTIuNDU4LDIuNDU4LDAsMCwxLDEyNC4wNzMsMjQuODIwMTlaIiB0cmFuc2Zvcm09InRyYW5zbGF0ZSgwIDApIi8+CiAgPGVsbGlwc2UgY2xhc3M9InRhbGVuZC1sb2dvIGJpZy1jaXJjbGUiIGN4PSIyNy44OTYxMSIgY3k9IjE2LjUzNjM0IiByeD0iNS45MzE4OSIgcnk9IjUuODMxMDMiLz4KPC9zdmc+Cg==") no-repeat 0 0;}
"""
cssOs.close()
