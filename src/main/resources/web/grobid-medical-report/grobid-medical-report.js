/**
*  Javascript functions for the front end.
*  Adapted from grobid-service (@author: Patrice Lopez)
*/

var grobid = (function($) {

	var teiToDownload;

	var block = 0;

    var elementCoords = ['s', 'persName', 'figure', 'head'];

	function defineBaseURL(ext) {
		var baseUrl = null;
        var localBase = $(location).attr('href');
		if ( localBase.indexOf("index.html") != -1) {
            localBase = localBase.replace("index.html", "");
        } 
        if (localBase.endsWith("#")) {
            localBase = localBase.substring(0,localBase.length-1);
        } 
		return localBase + "api/" + ext;
	}

	function setBaseUrl(ext) {
		var baseUrl = defineBaseURL(ext);
		if (block == 0)
			$('#gbdForm').attr('action', baseUrl);
		else if (block == 1)
			$('#gbdForm2').attr('action', baseUrl);
	}

	$(document).ready(function() {
		$("#subTitle").html("About");
		$("#divAbout").show();

		// for TEI-based results
        $("#divRestI").hide();

        // for PDF based results
        $("#divRestII").hide();

		$("#divInfo").hide();
		$('#consolidateBlock').show();
        $("#btn_download").hide();

		createInputFile();
		createInputFile2();

		setBaseUrl('processHeaderDocument');
		block = 0;

		$('#selectedService').change(function() {
			processChange();
			return true;
		});

		$('#selectedService2').change(function() {
			processChange();
			return true;
		});


		$('#gbdForm').ajaxForm({
            beforeSubmit: ShowRequest1,
            success: SubmitSuccesful,
            error: AjaxError1,
            dataType: "text"
        });

		$('#submitRequest2').bind('click', submitQuery2);

		// bind download buttons with download methods
		$('#btn_download').bind('click', download);
		$("#btn_download").hide();
        $('#btn_block_1').bind('click', downloadVisibilty);

		$("#about").click(function() {
			$("#about").attr('class', 'section-active');
			$("#rest").attr('class', 'section-not-active');
			$("#pdf").attr('class', 'section-not-active');
			$("#info").attr('class', 'section-not-active');

			$("#subTitle").html("About");
			$("#subTitle").show();

			$("#divAbout").show();
			$("#divRestI").hide();
			$("#divRestII").hide();
			$("#divInfo").hide();
			$("#divDemo").hide();
			return false;
		});
		$("#rest").click(function() {
			$("#rest").attr('class', 'section-active');
			$("#pdf").attr('class', 'section-not-active');
			$("#info").attr('class', 'section-not-active');
			$("#about").attr('class', 'section-not-active');

			$("#subTitle").hide();
			block = 0;
			processChange();

			$("#divRestI").show();
			$("#divRestII").hide();
			$("#divAbout").hide();
			$("#divInfo").hide();
			$("#divDemo").hide();
			return false;
		});

		$("#info").click(function() {
			$("#info").attr('class', 'section-active');
			$("#rest").attr('class', 'section-not-active');
			$("#pdf").attr('class', 'section-not-active');
			$("#about").attr('class', 'section-not-active');

			$("#subTitle").html("Info");
			$("#subTitle").show();

			$("#divInfo").show();
			$("#divAbout").hide();
			$("#divRestI").hide();
			$("#divRestII").hide();
			$("#divDemo").hide();
			return false;
		});
		$("#pdf").click(function() {
			$("#pdf").attr('class', 'section-active');
			$("#rest").attr('class', 'section-not-active');
			$("#about").attr('class', 'section-not-active');
			$("#info").attr('class', 'section-not-active');

			block = 1;
			setBaseUrl('referenceAnnotations');
			$("#subTitle").hide();
			processChange();

			$("#divInfo").hide();
			$("#divAbout").hide();
			$("#divRestI").hide();
			$("#divRestII").show();
			return false;
		});
	});

	function ShowRequest1(formData, jqForm, options) {
        var addCoordinates = false;
        for(var formd in formData) {
            if (formData[formd].name == 'teiCoordinates') {
                addCoordinates = true;
            }
        }
        if (addCoordinates) {
            for (var i in elementCoords) {
                var additionalFormData = {
                    "name": "teiCoordinates",
                    "value": "ref",
                    "type": "checkbox",
                    "required": false
                }
                additionalFormData["value"] = elementCoords[i]
                formData.push(additionalFormData)
            }
        }
	    $('#requestResult').html('<font color="grey">Requesting server...</font>');
	    return true;
	}

	function ShowRequest2(formData, jqForm, options) {
	    $('#infoResult2').html('<font color="grey">Requesting server...</font>');
	    return true;
	}

	function AjaxError1(jqXHR, textStatus, errorThrown) {
		$('#requestResult').html("<font color='red'>Error encountered while requesting the server.<br/>"+jqXHR.responseText+"</font>");
		responseJson = null;
	}

    function AjaxError2(message) {
    	if (!message)
    		message ="";
    	message += " - The PDF document cannot be annotated. Please check the server logs.";
    	$('#infoResult2').html("<font color='red'>Error encountered while requesting the server.<br/>"+message+"</font>");
		responseJson = null;
        return true;
    }

    function AjaxError21(message) {
        if (!message)
            message ="";
        $('#infoResult2').html("<font color='red'>Error encountered while requesting the server.<br/>"+message+"</font>");
        responseJson = null;
        return true;
    }


	function htmll(s) {
    	return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  	}

	function SubmitSuccesful(responseText, statusText, xhr) {
		var display = "<pre class='prettyprint lang-xml' id='xmlCode'>";
		var testStr = vkbeautify.xml(responseText);
        teiToDownload = responseText;
		display += htmll(testStr);

		display += "</pre>";
		$('#requestResult').html(display);
		window.prettyPrint && prettyPrint();
		$('#requestResult').show();
        $("#btn_download").show();
	}

    function submitQuery2() {
        var selected = $('#selectedService2 option:selected').attr('value');
        if (selected == 'annotatePDF') {

            var form = document.getElementById('gbdForm2');
            var formData = new FormData(form);
            var xhr = new XMLHttpRequest();
            var url = $('#gbdForm2').attr('action');
            xhr.responseType = 'arraybuffer';
            xhr.open('POST', url, true);
            ShowRequest2();
            xhr.onreadystatechange = function (e) {
                if (xhr.readyState == 4) {
                    if (xhr.status == 200) {
                        var response = e.target.response;
                        var pdfAsArray = new Uint8Array(response);
                        // Use PDFJS to render a pdfDocument from pdf array
                        var frame = '<iframe id="pdfViewer" src="resources/pdf.js/web/viewer.html?file=" style="width: 100%; height: 1000px;"></iframe>';
                        $('#requestResult2').html();
                        $('#infoResult2').html(frame);
                        var pdfjsframe = document.getElementById('pdfViewer');
                        pdfjsframe.onload = function () {
                            pdfjsframe.contentWindow.PDFViewerApplication.open(pdfAsArray);
                        };
                    } else {
                        AjaxError2("Response " + xhr.status + ": " );
                    }
                }
            };
            xhr.send(formData);  // multipart/form-data
        } else {
            // we will have JSON annotations to be layered on the PDF

            // request for the annotation information
            var form = document.getElementById('gbdForm2');
            var formData = new FormData(form);
            var xhr = new XMLHttpRequest();
            var url = $('#gbdForm2').attr('action');
            xhr.responseType = 'json';
            xhr.open('POST', url, true);
            ShowRequest2();

            var nbPages = -1;

            // display the local PDF
            if ((document.getElementById("input2").files[0].type == 'application/pdf') ||
                (document.getElementById("input2").files[0].name.endsWith(".pdf")) ||
                (document.getElementById("input2").files[0].name.endsWith(".PDF"))) {
                var reader = new FileReader();
                reader.onloadend = function () {
                    // to avoid cross origin issue
                    //PDFJS.disableWorker = true;
                    var pdfAsArray = new Uint8Array(reader.result);
                    // Use PDFJS to render a pdfDocument from pdf array
                    PDFJS.getDocument(pdfAsArray).then(function (pdf) {
                        // Get div#container and cache it for later use
                        var container = document.getElementById("requestResult2");
                        // enable hyperlinks within PDF files.
                        //var pdfLinkService = new PDFJS.PDFLinkService();
                        //pdfLinkService.setDocument(pdf, null);

                        $('#requestResult2').html('');
                        nbPages = pdf.numPages;

                        // Loop from 1 to total_number_of_pages in PDF document
                        for (var i = 1; i <= nbPages; i++) {

                            // Get desired page
                            pdf.getPage(i).then(function (page) {

                                var div0 = document.createElement("div");
                                div0.setAttribute("style", "text-align: center; margin-top: 1cm;");
                                var pageInfo = document.createElement("p");
                                var t = document.createTextNode("page " + (page.pageIndex + 1) + "/" + (nbPages));
                                pageInfo.appendChild(t);
                                div0.appendChild(pageInfo);
                                container.appendChild(div0);

                                var scale = 1.5;
                                var viewport = page.getViewport(scale);
                                var div = document.createElement("div");

                                // Set id attribute with page-#{pdf_page_number} format
                                div.setAttribute("id", "page-" + (page.pageIndex + 1));

                                // This will keep positions of child elements as per our needs, and add a light border
                                div.setAttribute("style", "position: relative; border-style: solid; border-width: 1px; border-color: gray;");

                                // Append div within div#container
                                container.appendChild(div);

                                // Create a new Canvas element
                                var canvas = document.createElement("canvas");

                                // Append Canvas within div#page-#{pdf_page_number}
                                div.appendChild(canvas);

                                var context = canvas.getContext('2d');
                                canvas.height = viewport.height;
                                canvas.width = viewport.width;

                                var renderContext = {
                                    canvasContext: context,
                                    viewport: viewport
                                };

                                // Render PDF page
                                page.render(renderContext).then(function () {
                                    // Get text-fragments
                                    return page.getTextContent();
                                })
                                    .then(function (textContent) {
                                        // Create div which will hold text-fragments
                                        var textLayerDiv = document.createElement("div");

                                        // Set it's class to textLayer which have required CSS styles
                                        textLayerDiv.setAttribute("class", "textLayer");

                                        // Append newly created div in `div#page-#{pdf_page_number}`
                                        div.appendChild(textLayerDiv);

                                        // Create new instance of TextLayerBuilder class
                                        var textLayer = new TextLayerBuilder({
                                            textLayerDiv: textLayerDiv,
                                            pageIndex: page.pageIndex,
                                            viewport: viewport
                                        });

                                        // Set text-fragments
                                        textLayer.setTextContent(textContent);

                                        // Render text-fragments
                                        textLayer.render();
                                    });
                            });
                        }
                    });
                }
                reader.readAsArrayBuffer(document.getElementById("input2").files[0]);

                xhr.onreadystatechange = function (e) {
                    if (xhr.readyState == 4 && xhr.status == 200) {
                        var response = e.target.response;
                        //var response = JSON.parse(xhr.responseText);
                        //console.log(response);
                        setupAnnotations(response);
                    } else if (xhr.status != 200) {
                        AjaxError2("Response " + xhr.status + ": ");
                    }
                };
                xhr.send(formData);
            } else {
                AjaxError21("This does not look like a PDF");
            }
        }
    }

	function setupAnnotations(response) {
		// we must check/wait that the corresponding PDF page is rendered at this point
		$('#infoResult2').html('');
		var json = response;
		var pageInfo = json.pages;

		var page_height = 0.0;
		var page_width = 0.0;

        // formulas
        var formulas = json.formulas;
        var mapFormulas = {};
        if (formulas) {
            for(var n in formulas) {
                var annotation = formulas[n];
                var theId = annotation.id;
                var pos = annotation.pos;
                if (pos)
                    mapFormulas[theId] = annotation;
                //for (var m in pos) {
                pos.forEach(function(thePos, m) {
                    //var thePos = pos[m];
                    // get page information for the annotation
                    var pageNumber = thePos.p;
                    if (pageInfo[pageNumber-1]) {
                        page_height = pageInfo[pageNumber-1].page_height;
                        page_width = pageInfo[pageNumber-1].page_width;
                    }
                    annotateFormula(true, theId, thePos, null, page_height, page_width, null);
                });
            }
        }

        var formulaMarkers = json.formulaMarkers;
		if (formulaMarkers) {
			formulaMarkers.forEach(function(annotation, n) {
				var theId = annotation.id;
				//if (!theId)
                //    return;
				// we take the first and last positions
				var targetFormula = null;
				if (theId)
					targetFormula = mapFormulas[theId];
				if (targetFormula) {
					var theFormulaPos = {};
					var pos = targetFormula.pos;
					if (pos.length == 1) 
						theFormulaPos = pos[0]
					else {
						//if (pos && (pos.length > 0)) {
						var theFirstPos = pos[0];
						var theLastPos = pos[pos.length-1];
						theFormulaPos.p = theFirstPos.p;
						theFormulaPos.w = Math.max(theFirstPos.w, theLastPos.w);
						theFormulaPos.h = Math.max(Math.abs(theLastPos.y - theFirstPos.y), theFirstPos.h) + Math.max(theFirstPos.h, theLastPos.h);
						theFormulaPos.x = Math.min(theFirstPos.x, theLastPos.x);
						theFormulaPos.y = Math.min(theFirstPos.y, theLastPos.y);
					}
					var pageNumber = theFormulaPos.p;
					if (pageInfo[pageNumber-1]) {
						page_height = pageInfo[pageNumber-1].page_height;
						page_width = pageInfo[pageNumber-1].page_width;
					}
					annotateFormula(false, theId, annotation, null, page_height, page_width, theFormulaPos);
					//}
				} else {
					var pageNumber = annotation.p;
					if (pageInfo[pageNumber-1]) {
						page_height = pageInfo[pageNumber-1].page_height;
						page_width = pageInfo[pageNumber-1].page_width;
					}
					annotateFormula(false, theId, annotation, null, page_height, page_width, null);
				}
			});
		}

        // figures
        var figures = json.figures;
        var mapFigures = {};
        if (figures) {
            for(var n in figures) {
                var annotation = figures[n];
                var theId = annotation.id;
                var pos = annotation.pos;
                if (pos)
                    mapFigures[theId] = annotation;
                pos.forEach(function(thePos, m) {
                    // get page information for the annotation
                    var pageNumber = thePos.p;
                    if (pageInfo[pageNumber-1]) {
                        page_height = pageInfo[pageNumber-1].page_height;
                        page_width = pageInfo[pageNumber-1].page_width;
                    }
                    annotateFigure(true, theId, thePos, null, page_height, page_width, null);
                });
            }
        }

        var figureMarkers = json.figureMarkers;
        if (figureMarkers) {
            figureMarkers.forEach(function(annotation, n) {
                var theId = annotation.id;
                // we take the first and last positions
                var targetFigure = null;
                if (theId)
                    targetFigure = mapFigures[theId];
                if (targetFigure) {
                    var theFigurePos = {};
                    var pos = targetFigure.pos;
                    if (pos.length == 1) 
                        theFigurePos = pos[0];
                    else {
                        // for figure we have to scan all the component positions, because graphic objects are not sorted
                        var theFirstPos = pos[0];
                        theFigurePos.p = theFirstPos.p;
                        theFigurePos.x = theFirstPos.x;
                        theFigurePos.y = theFirstPos.y;
                        theFigurePos.h = theFirstPos.h;
                        theFigurePos.w = theFirstPos.w;

                        for (thePosIndex in pos) {
                            if (thePosIndex == 0)
                                continue
                            thePos = pos[thePosIndex]
                            if (thePos.x < theFigurePos.x) {
                                theFigurePos.w = theFigurePos.w + (theFigurePos.x - thePos.x);
                                theFigurePos.x = thePos.x;
                            }
                            if (thePos.y < theFigurePos.y) {
                                theFigurePos.h = theFigurePos.h + (theFigurePos.y - thePos.y);
                                theFigurePos.y = thePos.y;
                            }

                            var maxFigureX = theFigurePos.x + theFigurePos.w;
                            var maxFigureY = theFigurePos.y + theFigurePos.h;

                            var maxPosX = thePos.x + thePos.w;
                            var maxPosY = thePos.y + thePos.h;

                            if (maxPosX > maxFigureX) {
                                theFigurePos.w = maxPosX - theFigurePos.x;
                            }

                            if (maxPosY > maxFigureY) {
                                theFigurePos.h = maxPosY - theFigurePos.y;
                            }
                        }
                    }
                    var pageNumber = theFigurePos.p;
                    if (pageInfo[pageNumber-1]) {
                        page_height = pageInfo[pageNumber-1].page_height;
                        page_width = pageInfo[pageNumber-1].page_width;
                    }
                    annotateFigure(false, theId, annotation, null, page_height, page_width, theFigurePos);
                } else {
                    var pageNumber = annotation.p;
                    if (pageInfo[pageNumber-1]) {
                        page_height = pageInfo[pageNumber-1].page_height;
                        page_width = pageInfo[pageNumber-1].page_width;
                    }
                    annotateFigure(false, theId, annotation, null, page_height, page_width, null);
                }
            });
        }

        // tables
        var tables = json.tables;
        var mapTables = {};
        if (tables) {
            for(var n in tables) {
                var annotation = tables[n];
                var theId = annotation.id;
                var pos = annotation.pos;
                if (pos)
                    mapTables[theId] = annotation;
                pos.forEach(function(thePos, m) {
                    // get page information for the annotation
                    var pageNumber = thePos.p;
                    if (pageInfo[pageNumber-1]) {
                        page_height = pageInfo[pageNumber-1].page_height;
                        page_width = pageInfo[pageNumber-1].page_width;
                    }
                    annotateTable(true, theId, thePos, null, page_height, page_width, null);
                });
            }
        }

        var tableMarkers = json.tableMarkers;
        if (tableMarkers) {
            tableMarkers.forEach(function(annotation, n) {
                var theId = annotation.id;
                //if (!theId)
                //    return;
                // we take the first and last positions
                var targetTable = null;
                if (theId)
                    targetTable = mapTables[theId];
                if (targetTable) {
                    var theTablePos = {};
                    var pos = targetTable.pos;
                    if (pos.length == 1) 
                        theTablePos = pos[0]
                    else {
                        //if (pos && (pos.length > 0)) {
                        var theFirstPos = pos[0];
                        var theLastPos = pos[pos.length-1];
                        theTablePos.p = theFirstPos.p;
                        theTablePos.w = Math.max(theFirstPos.w, theLastPos.w);
                        theTablePos.h = Math.max(Math.abs(theLastPos.y - theFirstPos.y), theFirstPos.h) + Math.max(theFirstPos.h, theLastPos.h);
                        theTablePos.x = Math.min(theFirstPos.x, theLastPos.x);
                        theTablePos.y = Math.min(theFirstPos.y, theLastPos.y);
                    }
                    var pageNumber = theTablePos.p;
                    if (pageInfo[pageNumber-1]) {
                        page_height = pageInfo[pageNumber-1].page_height;
                        page_width = pageInfo[pageNumber-1].page_width;
                    }
                    annotateTable(false, theId, annotation, null, page_height, page_width, theTablePos);
                    //}
                } else {
                    var pageNumber = annotation.p;
                    if (pageInfo[pageNumber-1]) {
                        page_height = pageInfo[pageNumber-1].page_height;
                        page_width = pageInfo[pageNumber-1].page_width;
                    }
                    annotateTable(false, theId, annotation, null, page_height, page_width, null);
                }
            });
        }

        var refBibs = json.refBibs;
        var mapRefBibs = {};
        if (refBibs) {
            for(var n in refBibs) {
                var annotation = refBibs[n];
                var theId = annotation.id;
                var theUrl = annotation.url;
                var pos = annotation.pos;
                if (pos)
                    mapRefBibs[theId] = annotation;
                //for (var m in pos) {
                pos.forEach(function(thePos, m) {
                    //var thePos = pos[m];
                    // get page information for the annotation
                    var pageNumber = thePos.p;
                    if (pageInfo[pageNumber-1]) {
                        page_height = pageInfo[pageNumber-1].page_height;
                        page_width = pageInfo[pageNumber-1].page_width;
                    }
                    annotateBib(true, theId, thePos, theUrl, page_height, page_width, null);
                });
            }
        }

        // we need the above mapRefBibs structure to be created to perform the ref. markers analysis
        var refMarkers = json.refMarkers;
        if (refMarkers) {
            //for(var n in refMarkers) {
            refMarkers.forEach(function(annotation, n) {
                //var annotation = refMarkers[n];
                var theId = annotation.id;
                //if (!theId)
                //    return;
                // we take the first and last positions
                var targetBib = mapRefBibs[theId];
                if (targetBib) {
                    var theBibPos = {};
                    var pos = targetBib.pos;
                    if (pos.length == 1) 
                        theBibPos = pos[0];
                    else {
                        //if (pos && (pos.length > 0)) {
                        var theFirstPos = pos[0];
                        // we can't visualize over two pages, so we take as theLastPos the last coordinate position on the page of theFirstPos
                        
                        var theLastPos = pos[pos.length-1];
                        if (theLastPos.p != theFirstPos.p) {
                            var k = 2;
                            while (pos.length-k>0) {
                                theLastPos = pos[pos.length-k];
                                if (theLastPos.p == theFirstPos.p) 
                                    break;
                                k++;
                            }
                        }
                        theBibPos.p = theFirstPos.p;
                        theBibPos.w = Math.max(theFirstPos.w, theLastPos.w);
                        theBibPos.h = Math.max(Math.abs(theLastPos.y - theFirstPos.y), theFirstPos.h) + Math.max(theFirstPos.h, theLastPos.h);
                        theBibPos.x = Math.min(theFirstPos.x, theLastPos.x);
                        theBibPos.y = Math.min(theFirstPos.y, theLastPos.y);
                    }
                    var pageNumber = theBibPos.p;
                    if (pageInfo[pageNumber-1]) {
                        page_height = pageInfo[pageNumber-1].page_height;
                        page_width = pageInfo[pageNumber-1].page_width;
                    }
                    annotateBib(false, theId, annotation, null, page_height, page_width, theBibPos);
                    //}
                } else {
                    var pageNumber = annotation.p;
                    if (pageInfo[pageNumber-1]) {
                        page_height = pageInfo[pageNumber-1].page_height;
                        page_width = pageInfo[pageNumber-1].page_width;
                    }
                    annotateBib(false, theId, annotation, null, page_height, page_width, null);
                }
            });
        }
	}

	function annotateBib(bib, theId, thePos, url, page_height, page_width, theBibPos) {
		var page = thePos.p;
		var pageDiv = $('#page-'+page);
		var canvas = pageDiv.children('canvas').eq(0);;

		var canvasHeight = canvas.height();
		var canvasWidth = canvas.width();
		var scale_x = canvasHeight / page_height;
		var scale_y = canvasWidth / page_width;

		var x = thePos.x * scale_x;
		var y = thePos.y * scale_y;
		var width = thePos.w * scale_x;
		var height = thePos.h * scale_y;

		//make clickable the area
		var element = document.createElement("a");
		var attributes = "display:block; width:"+width+"px; height:"+height+"px; position:absolute; top:"+y+"px; left:"+x+"px;";

		if (bib) {
			// this is a bibliographical reference
			// we draw a line
			if (url) {
				element.setAttribute("style", attributes + "border:2px; border-style:none none solid none; border-color: blue;");
				element.setAttribute("href", url);
				element.setAttribute("target", "_blank");
			}
			else
				element.setAttribute("style", attributes + "border:1px; border-style:none none dotted none; border-color: gray;");
			element.setAttribute("id", theId);
		} else {
			// this is a reference marker
			// we draw a box (blue if associated to an id, gray otherwise)			
			if (theId) {
                element.setAttribute("style", attributes + "border:1px solid; border-color: blue;");
                // the link here goes to the bibliographical reference
				element.onclick = function() {
					goToByScroll(theId);
				};
			} else
                element.setAttribute("style", attributes + "border:1px solid; border-color: gray;");

			// we need the area where the actual target bibliographical reference is
			if (theBibPos) {
				element.setAttribute("data-toggle", "popover");
				element.setAttribute("data-placement", "top");
				element.setAttribute("data-content", "content");
				element.setAttribute("data-trigger", "hover");
				var newWidth = theBibPos.w * scale_x;
				var newHeight = theBibPos.h * scale_y;
				var newImg = getImagePortion(theBibPos.p, newWidth, newHeight, theBibPos.x * scale_x, theBibPos.y * scale_y);
				$(element).popover({
					content:  function () {
						return '<img src=\"'+ newImg + '\" style=\"width:100%\" />';
						//return '<img src=\"'+ newImg + '\" />';
					},
					html: true,
					container: 'body'
					/*width: newWidth + 'px',
					height: newHeight + 'px'
					container: canvas,
					width: '600px',
					height: '100px'*/
    			});
			}
		}
		pageDiv.append(element);
	}

    function annotateFormula(formula, theId, thePos, url, page_height, page_width, theFormulaPos) {
        var page = thePos.p;
        var pageDiv = $('#page-'+page);
        var canvas = pageDiv.children('canvas').eq(0);;

        var canvasHeight = canvas.height();
        var canvasWidth = canvas.width();
        var scale_x = canvasHeight / page_height;
        var scale_y = canvasWidth / page_width;

        var x = thePos.x * scale_x;
        var y = thePos.y * scale_y;
        var width = thePos.w * scale_x;
        var height = thePos.h * scale_y;

        /*console.log('annotate: ' + page + " " + x + " " + y + " " + width + " " + height);
        console.log('location: ' + canvasHeight + " " + canvasWidth);
        console.log('location: ' + page_height + " " + page_width);*/
        //make clickable the area
        var element = document.createElement("a");
        var attributes = "display:block; width:"+width+"px; height:"+height+"px; position:absolute; top:"+y+"px; left:"+x+"px;";

        if (formula) {
            // this is a formula
            // we draw a line
            element.setAttribute("style", attributes + "border:1px; border-style:dotted; border-color: red;");
            element.setAttribute("title", "formula");
            element.setAttribute("id", theId);
        } else {
            // this is a formula reference marker    
            // we draw a box (red if associated to an id, gray otherwise)
            if (theId) {
                element.setAttribute("style", attributes + "border:1px solid; border-color: red;");
                // the link here goes to the referenced formula
                element.onclick = function() {
                    goToByScroll(theId);
                };
            } else
                 element.setAttribute("style", attributes + "border:1px solid; border-color: gray;");

            // we need the area where the actual target formula is
            if (theFormulaPos) {
                element.setAttribute("data-toggle", "popover");
                element.setAttribute("data-placement", "top");
                element.setAttribute("data-content", "content");
                element.setAttribute("data-trigger", "hover");
                var newWidth = theFormulaPos.w * scale_x;
                var newHeight = theFormulaPos.h * scale_y;
                var newImg = getImagePortion(theFormulaPos.p, newWidth, newHeight, theFormulaPos.x * scale_x, theFormulaPos.y * scale_y);
                $(element).popover({
                    content:  function () {
                        return '<img src=\"'+ newImg + '\" style=\"width:100%\" />';
                        //return '<img src=\"'+ newImg + '\" />';
                    },
                    html: true,
                    container: 'body'
                    /*width: newWidth + 'px',
                    height: newHeight + 'px'
                    container: canvas,
                    width: '600px',
                    height: '100px'*/
                });
            }
        }
        pageDiv.append(element);
    }

    function annotateFigure(figure, theId, thePos, url, page_height, page_width, theFigurePos) {
        var page = thePos.p;
        var pageDiv = $('#page-'+page);
        var canvas = pageDiv.children('canvas').eq(0);;

        var canvasHeight = canvas.height();
        var canvasWidth = canvas.width();
        var scale_x = canvasHeight / page_height;
        var scale_y = canvasWidth / page_width;

        var x = thePos.x * scale_x;
        var y = thePos.y * scale_y;
        var width = thePos.w * scale_x;
        var height = thePos.h * scale_y;

        /*console.log('annotate: ' + page + " " + x + " " + y + " " + width + " " + height);
        console.log('location: ' + canvasHeight + " " + canvasWidth);
        console.log('location: ' + page_height + " " + page_width);*/
        //make clickable the area
        var element = document.createElement("a");
        var attributes = "display:block; width:"+width+"px; height:"+height+"px; position:absolute; top:"+y+"px; left:"+x+"px;";

        if (figure) {
            // this is a figure
            // we draw a line
            element.setAttribute("style", attributes + "border:1px; border-style:dotted; border-color: blue;");
            element.setAttribute("title", "figure");
            element.setAttribute("id", theId);
        } else {
            // this is a figure reference marker    
            // we draw a box (blue if associated to an id, gray otherwise)
            if (theId) {
                element.setAttribute("style", attributes + "border:1px solid; border-color: blue;");
                // the link here goes to the referenced figure
                element.onclick = function() {
                    goToByScroll(theId);
                };
            } else
                 element.setAttribute("style", attributes + "border:1px solid; border-color: gray;");

            // we need the area where the actual target figure is
            if (theFigurePos) {
                element.setAttribute("data-toggle", "popover");
                element.setAttribute("data-placement", "top");
                element.setAttribute("data-content", "content");
                element.setAttribute("data-trigger", "hover");
                var newWidth = theFigurePos.w * scale_x;
                var newHeight = theFigurePos.h * scale_y;
                var newImg = getImagePortion(theFigurePos.p, newWidth, newHeight, theFigurePos.x * scale_x, theFigurePos.y * scale_y);
                $(element).popover({
                    content:  function () {
                        return '<img src=\"'+ newImg + '\" style=\"width:100%\" />';
                        //return '<img src=\"'+ newImg + '\" />';
                    },
                    html: true,
                    container: 'body'
                   /* width: newWidth + 'px',
                    height: newHeight + 'px'
                    container: canvas,
                    width: '600px',
                    height: '100px'*/
                });
            }
        }
        pageDiv.append(element);
    }

    function annotateTable(table, theId, thePos, url, page_height, page_width, theTablePos) {
        var page = thePos.p;
        var pageDiv = $('#page-'+page);
        var canvas = pageDiv.children('canvas').eq(0);;

        var canvasHeight = canvas.height();
        var canvasWidth = canvas.width();
        var scale_x = canvasHeight / page_height;
        var scale_y = canvasWidth / page_width;

        var x = thePos.x * scale_x;
        var y = thePos.y * scale_y;
        var width = thePos.w * scale_x;
        var height = thePos.h * scale_y;

        /*console.log('annotate: ' + page + " " + x + " " + y + " " + width + " " + height);
        console.log('location: ' + canvasHeight + " " + canvasWidth);
        console.log('location: ' + page_height + " " + page_width);*/
        //make clickable the area
        var element = document.createElement("a");
        var attributes = "display:block; width:"+width+"px; height:"+height+"px; position:absolute; top:"+y+"px; left:"+x+"px;";

        if (table) {
            // this is a table
            // we draw a line
            element.setAttribute("style", attributes + "border:1px; border-style:dotted; border-color: blue;");
            element.setAttribute("title", "table");
            element.setAttribute("id", theId);
        } else {
            // this is a table reference marker    
            // we draw a box (blue if associated to an id, gray otherwise)
            if (theId) {
                element.setAttribute("style", attributes + "border:1px solid; border-color: blue;");
                // the link here goes to the referenced table
                element.onclick = function() {
                    goToByScroll(theId);
                };
            } else
                 element.setAttribute("style", attributes + "border:1px solid; border-color: gray;");

            // we need the area where the actual target table is
            if (theTablePos) {
                element.setAttribute("data-toggle", "popover");
                element.setAttribute("data-placement", "top");
                element.setAttribute("data-content", "content");
                element.setAttribute("data-trigger", "hover");
                var newWidth = theTablePos.w * scale_x;
                var newHeight = theTablePos.h * scale_y;
                var newImg = getImagePortion(theTablePos.p, newWidth, newHeight, theTablePos.x * scale_x, theTablePos.y * scale_y);
                $(element).popover({
                    content:  function () {
                        return '<img src=\"'+ newImg + '\" style=\"width:100%\" />';
                        //return '<img src=\"'+ newImg + '\" />';
                    },
                    html: true,
                    container: 'body'
                    /*width: newWidth + 'px',
                    height: newHeight + 'px'
                    container: canvas,
                    width: '600px',
                    height: '100px'*/
                });
            }
        }
        pageDiv.append(element);
    }

	/* jquery-based movement to an anchor, without modifying the displayed url and a bit smoother */
	function goToByScroll(id) {
    	$('html,body').animate({scrollTop: $("#"+id).offset().top},'fast');
	}

	/* cropping an area from a canvas */
	function getImagePortion(page, width, height, x, y) {
        //console.log("page: " + page + ", width: " + width + ", height: " + height + ", x: " + x + ", y: " + y);
		// get the page div
		var pageDiv = $('#page-'+page);
        //console.log(page);
		// get the source canvas
		var canvas = pageDiv.children('canvas')[0];
		// the destination canvas
		var tnCanvas = document.createElement('canvas');
 		var tnCanvasContext = tnCanvas.getContext('2d');
 		tnCanvas.width = width;
    	tnCanvas.height = height;
		tnCanvasContext.drawImage(canvas, x , y, width, height, 0, 0, width, height);
 		return tnCanvas.toDataURL();
	}

	$(document).ready(function() {
	    $(document).on('shown', '#xmlCode', function(event) {
	        prettyPrint();
	    });
	});

	function processChange() {
		var selected = $('#selectedService option:selected').attr('value');
		if (block == 1)
			selected = $('#selectedService2 option:selected').attr('value');

		if (selected == 'processHeaderDocument') {
			createInputFile(selected);
            $('#segmentSentencesBlock').hide();
            $('#teiCoordinatesBlock').hide();
			setBaseUrl('processHeaderDocument');
		}
		else if (selected == 'processLeftNoteDocument') {
            createInputFile(selected);
            $('#segmentSentencesBlock').hide();
            $('#teiCoordinatesBlock').hide();
            setBaseUrl('processLeftNoteDocument');
        }
		else if (selected == 'processFullMedicalText') {
			createInputFile(selected);
            $('#segmentSentencesBlock').show();
            $('#teiCoordinatesBlock').show();
			setBaseUrl('processFullMedicalText');
		}
		else if (selected == 'processFrenchMedicalNER') {
            createInputFile(selected);
            $('#segmentSentencesBlock').hide();
            $('#teiCoordinatesBlock').hide();
            setBaseUrl('processFrenchMedicalNER');
        }
		else if (selected == 'processDateline') {
			createInputTextArea('dateline');
			$('#textInputArea').attr('placeholder', 'Example:\n Paris, le 15.12.2019. Intervention du 01/12/2019.');
            $('#segmentSentencesBlock').hide();
            $('#teiCoordinatesBlock').hide();
			setBaseUrl('processDateline');
		}
		else if (selected == 'processMedic') {
			createInputTextArea('medic');
			$('#textInputArea').attr('placeholder', 'Example:\n Chef de Service Pr. Abagael ZOSIMA. Assistant Dr Woody WOOD.');
            $('#segmentSentencesBlock').hide();
            $('#teiCoordinatesBlock').hide();
			setBaseUrl('processMedic');
		}
		else if (selected == 'processPatient') {
			createInputTextArea('patient');
			$('#textInputArea').attr('placeholder', 'Example:\n Madame Eva GOODRICH. 666, RUE DU MARRANT 92290 CHATENAY MALABRY.');
            $('#segmentSentencesBlock').hide();
            $('#teiCoordinatesBlock').hide();
			setBaseUrl('processPatient');
		}
		else if (selected == 'processNER') {
            createInputTextArea('ner');
            $('#textInputArea').attr('placeholder', 'The text sample can be found here: resources/test/MedicalTextExample.txt');
            $('#segmentSentencesBlock').hide();
            $('#teiCoordinatesBlock').hide();
            setBaseUrl('processNER');
        }
		else if (selected == 'mentionAnnotations') {
			createInputFile2(selected);
			$('#consolidateBlockPDFRef').hide();
            $('#consolidateBlockPDFFig').show();
			setBaseUrl('mentionAnnotations');
		}
		else if (selected == 'annotatePDF') {
			createInputFile2(selected);
			$('#consolidateBlockPDFRef').hide();
            $('#consolidateBlockPDFFig').hide();
			setBaseUrl('annotatePDF');
		}
	}

	function createInputFile(selected) {
		$('#textInputDiv').hide();
		$('#fileInputDiv').show();

		$('#gbdForm').attr('enctype', 'multipart/form-data');
		$('#gbdForm').attr('method', 'post');
	}

	function createInputFile2(selected) {
		$('#textInputDiv2').hide();
		$('#fileInputDiv2').show();

		$('#gbdForm2').attr('enctype', 'multipart/form-data');
		$('#gbdForm2').attr('method', 'post');
	}

	function createInputTextArea(nameInput) {
		$('#fileInputDiv').hide();
		$('#textInputArea').attr('name', nameInput);
		$('#textInputDiv').show();

		$('#gbdForm').attr('enctype', '');
		$('#gbdForm').attr('method', 'post');
	}

	function download(){
        var name ="export";
		if (document.getElementById("input")
            && document.getElementById("input").files.length > 0
                && document.getElementById("input").files[0].name) {
             name = document.getElementById("input").files[0].name;
        }

		var fileName = name + ".tei.xml";
	    var a = document.createElement("a");

	    var file = new Blob([teiToDownload], {type: 'application/xml'});
	    var fileURL = URL.createObjectURL(file);
	    a.href = fileURL;
	    a.download = fileName;

	    document.body.appendChild(a);

	    $(a).ready(function() {
			a.click();
			return true;
		});
	}
    })(jQuery);


function downloadVisibilty(){
    $("#btn_download").hide();
}
