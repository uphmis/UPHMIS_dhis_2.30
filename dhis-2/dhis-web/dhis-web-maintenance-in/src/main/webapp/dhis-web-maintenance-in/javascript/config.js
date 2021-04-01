
function clearFolder( folderId )
{
	/*
	var request = new Request();
	request.setResponseTypeXML( 'message' );
	request.setCallbackSuccess( clearFolderRecieved );
	  
	var requestString = "clearFolder.action";
	var params = 'selectedButton=' + folderId;	
	
	request.sendAsPost( params );
	request.send( requestString );
	*/
	$.post("clearFolder.action",
			{
				selectedButton : folderId
			},
			function (data)
			{
				clearFolderRecieved(data);
			},'xml');
	
}

function clearFolderRecieved( xmlObject )
{
	//var message = messageElement.firstChild.nodeValue;

    //document.getElementById( 'message' ).innerHTML = message;
    document.getElementById( 'message' ).style.display = 'block';
    
    var responseMsgs = xmlObject.getElementsByTagName("message");
    
    for ( var i = 0; i < responseMsgs.length; i++ )
    {
        var responseMsg = responseMsgs[ i ].getElementsByTagName("statusMessage")[0].firstChild.nodeValue;
		
		//document.reportForm.ouNameTB.value = orgUnitName;
		//document.reportForm.ouLevelTB.value = level;	
		//document.getElementById( 'responseMessage' ).style.display = 'block';
	    document.getElementById( 'message' ).innerHTML = responseMsg;
    }  
    
    
}


function downloadFolder( folderId )
{
	window.location.href="clearFolder.action?selectedButton="+folderId;
}