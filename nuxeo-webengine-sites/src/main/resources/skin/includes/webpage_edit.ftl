<#macro webPageEdit>

<!-- markitup -->
<script src="${skinPath}/script/markitup/jquery.markitup.pack.js"></script>
<link rel="stylesheet" type="text/css" href="${skinPath}/script/markitup/skins/markitup/style.css" />
<link rel="stylesheet" type="text/css" href="${skinPath}/script/markitup/sets/wiki/style.css" />
<!-- end markitup -->

<!-- tinyMCE -->
<script type="text/javascript" src="../../../${skinPath}/script/tiny_mce/tiny_mce.js"></script>
<script type="text/javascript" src="../../../${skinPath}/script/tiny_mce/langs/en.js"></script>
<script type="text/javascript" src="../../../${skinPath}/script/tiny_mce/themes/simple/editor_template.js"></script>
<!-- end tinyMCE -->

<form name="pageEdit" method="POST" action="${This.path}/modifyWebPage" accept-charset="utf-8">
  <table class="modifyWebPage">
    <tbody>
      <tr>
        <td width="30%"></td>
        <td width="70%"></td>
      </tr>
      <tr>
        <td>${Context.getMessage("label.page.title")}</td>
        <td><input type="text" name="title" value="${Document.title}"/></td>
      </tr>
      <tr>
        <td>${Context.getMessage("label.page.description")}</td>
        <td><textarea name="description">${Document.dublincore.description}</textarea></td>
      </tr>
      <tr>
        <td>${Context.getMessage("label.page.content")}</td>
        <td>
          <#if (Document.webpage.isRichtext == true)> 
            <textarea name="richtextEditorEdit" style="width: 300px;height: 400px" cols="60" rows="20" id="richtextEditorEdit">${Document.webpage.content}</textarea>
          <#else>
            <textarea name="wikitextEditorEdit" cols="60" rows="20" id="wikitextEditorEdit" >${Document.webpage.content}</textarea>
          </#if> 
        </td>
      </tr>
      <tr>
        <td>${Context.getMessage("label.page.push")}</td>
        <td>
          <input type="radio" id="pushToMenuYes" name="pushToMenu" value="true" />${Context.getMessage("label.page.push.yes")}
          <input type="radio" id="pushToMenuNo" name="pushToMenu" value="false" />${Context.getMessage("label.page.push.no")}
        </td>
      </tr>
      <tr>
        <td colspan="2">
          <input type="submit" value="${Context.getMessage("action_save")}" /> &nbsp;
          <input type="button" value="${Context.getMessage("action_cancel")}" onclick="document.pageEdit.action='${This.path}/@perspective/view'; document.pageEdit.submit();" />
        </td>
      </tr>
    </tbody>
  </table>  
</form>

<script>
function launchEditor() {
  mySitesWikiSettings = {
      nameSpace:          "wiki", // Useful to prevent multi-instances CSS conflict
      previewParserPath:   "${This.path}/@views/preview",
      previewParserVar: 'wiki_editor',
      previewAutorefresh: true,
      previewInWindow: 'width=500, height=700, resizable=yes, scrollbars=yes',
      onShiftEnter:       {keepDefault:false, replaceWith:'\n\n'},
      markupSet:  [
        {name:'Heading 1', key:'1', openWith:'== ', closeWith:' ==', placeHolder:'Your title here...' },
        {name:'Heading 2', key:'2', openWith:'=== ', closeWith:' ===', placeHolder:'Your title here...' },
        {name:'Heading 3', key:'3', openWith:'==== ', closeWith:' ====', placeHolder:'Your title here...' },
        {name:'Heading 4', key:'4', openWith:'===== ', closeWith:' =====', placeHolder:'Your title here...' },
        {name:'Heading 5', key:'5', openWith:'====== ', closeWith:' ======', placeHolder:'Your title here...' },
        {separator:'---------------' },
        {name:'Bold', key:'B', openWith:"**", closeWith:"**"},
        {name:'Italic', key:'I', openWith:"__", closeWith:"__"},
        //{name:'Stroke through', key:'S', openWith:'<s>', closeWith:'</s>'},
        {separator:'---------------' },
        {name:'Bulleted list', openWith:'(!(- |!|-)!)'},
        {name:'Numeric list', openWith:'(!(+ |!|+)!)'},
        {separator:'---------------' },
        {name:'Image', key:"T", replaceWith:'[[Image:[![Url:!:http://]!]|[![name]!]]]'},
        {name:'Link', key:"L", openWith:"[[![Link]!] ", closeWith:']', placeHolder:'Your text to link here...' },
        {name:'Url', openWith:"[[![Url:!:http://]!] ", closeWith:']', placeHolder:'Your text to link here...' },
        {separator:'---------------' },
        {name:'Quotes', openWith:'(!(> |!|>)!)'},
        {name:'Inline Code', openWith:'$$', closeWith:'$$'},
        {name:'Code', openWith:'{{{', closeWith:'}}}'},
        {separator:'---------------' },
        {name:'Preview', key: 'P', call:'preview', className:'preview'}
      ]
    };
  
  $('#wikitextEditorEdit').markItUp(mySitesWikiSettings);
}

$('#richtextEditorEdit').ready(function() {
  
  document.tmceEdit = new tinymce.Editor('richtextEditorEdit',{theme : "advanced",
    plugins : "safari,spellchecker,pagebreak,style,layer,table,save,advhr,advimage,advlink,emotions,iespell,inlinepopups,insertdatetime,preview,media",
    // Theme options
    theme_advanced_buttons1 : "save,newdocument,|,bold,italic,underline,strikethrough,|,justifyleft,justifycenter,justifyright,justifyfull,|,styleselect,formatselect,fontselect,fontsizeselect",
    theme_advanced_buttons2 : "cut,copy,paste,pastetext,pasteword,|,search,replace,|,bullist,numlist,|,outdent,indent,blockquote,|,undo,redo,|,link,unlink,anchor,image,cleanup,help,code,|,insertdate,inserttime,preview,|,forecolor,backcolor",
    theme_advanced_buttons3 : "tablecontrols,|,hr,removeformat,visualaid,|,sub,sup,|,charmap,emotions,iespell,media,advhr,|,print,|,ltr,rtl,|,fullscreen",
    theme_advanced_buttons4 : "insertlayer,moveforward,movebackward,absolute,|,styleprops,spellchecker,|,cite,abbr,acronym,del,ins,attribs,|,visualchars,nonbreaking,template,blockquote,pagebreak,|,insertfile,insertimage",
    theme_advanced_toolbar_location : "top",
    theme_advanced_toolbar_align : "left",
    theme_advanced_statusbar_location : "bottom",
    theme_advanced_resizing : true});
  

  document.tmceEdit.render();
});

$('#wikitextEditorEdit').ready(function() {
  setTimeout(launchEditor, 10);
});

if ('${Document.webpage.pushtomenu}' == 'true') {
  document.getElementById("pushToMenuYes").checked = true;
} else {
  document.getElementById("pushToMenuNo").checked = true;
}
</script>


</#macro>