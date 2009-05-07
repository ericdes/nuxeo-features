
// EDITOR

if (typeof NXThemesEditor == "undefined") {
    NXThemesEditor = {
        writeMessage: function(msg) {
          var box = $("nxthemesStatusMessage");
          box.innerHTML = msg;
          box.show();
          NXThemes.Effects.get('fadeout')(box, {delay: 1700});
        },
        isLowerCase: function(s) {
          for (var i = 0; i < s.length; i= i+1) {
            var c = s.charAt(i);
            if ( !((c>="a") && (c<="z")) ) {
              return false;
            }
          }
          return true;
        },
        isLowerCaseOrSpace: function(s) {
          for (var i = 0; i < s.length; i= i+1) {
            var c = s.charAt(i);
            if ( !((c>="a") && (c<="z") || c == ' ')) {
              return false;
            }
          }
          return true;
        }              
    };

}

NXThemesEditor.highlightSaveButton = function() {
	NXThemes.getViewById("save changes").highlight();
};


NXThemesEditor.dehighlightSaveButton = function() {
	NXThemes.getViewById("save changes").dehighlight();
};

NXThemesEditor.setCanvasMode =  function(info) {
    var target = Event.element(info);
    var mode = target.getAttribute("name");
    NXThemes.setCookie("nxthemes.mode", mode);
    NXThemes.getViewById("canvas mode selector").refresh();
    NXThemesEditor.refreshCanvas();
};

NXThemesEditor.deletePage = function(info) {
    var id =  info.target.getAttribute('id');
    if (id === null) {
      return;
    }
    var element_id = id.substr(4);
    var url = nxthemesBasePath + "/nxthemes-editor/delete_element";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             id: element_id
         },
         onSuccess: function(r) {
             NXThemes.getViewById("theme selector").refresh();
             NXThemesEditor.refreshCanvas();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         },         
    });
};

NXThemesEditor.moveElement = function(info) {
    var srcId = info.source.getAttribute('id');
    var destId = info.target.getAttribute('id');
    var order = info.order;
    var url = nxthemesBasePath + "/nxthemes-editor/move_element";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             src_id: srcId,
             dest_id: destId,
             order: order
         },
         onSuccess: function(r) {
             NXThemesEditor.refreshCanvas();
             NXThemesEditor.highlightSaveButton();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};

NXThemesEditor.addFragment = function(typeName, destId) {
    var url = nxthemesBasePath + "/nxthemes-editor/insert_fragment";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             type_name: typeName,
             dest_id: destId
         },
         onSuccess: function(r) {
             NXThemesEditor.highlightSaveButton();
             NXThemesEditor.editCanvas();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
    return false;
};

NXThemesEditor.editElement = function(info) {
    var id = info.target.getAttribute('id');
    var url = nxthemesBasePath + "/nxthemes-editor/select_element";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             'id': id
         },
         onSuccess: function(r) {
             NXThemes.getControllerById("editor perspectives").switchTo("edit element");
             NXThemes.getViewById("element editor tabs").switchTo("element editor perspectives/edit properties");             
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};

NXThemesEditor.insertFragment = function(info) {
    var id = info.target.getAttribute('id');
    var url = nxthemesBasePath + "/nxthemes-editor/select_element";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             'id': id
         },
         onSuccess: function(r) {
             NXThemes.getControllerById("editor perspectives").switchTo("add fragments");
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};

NXThemesEditor.changeElementStyle = function(info) {
    var id = info.target.getAttribute('id');
    var url = nxthemesBasePath + "/nxthemes-editor/select_element";

    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             'id': id
         },
         onSuccess: function(r) {
             NXThemes.getControllerById("editor perspectives").switchTo("edit element");
             NXThemes.getViewById("element editor tabs").switchTo("element editor perspectives/edit style");
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};

NXThemesEditor.setSize = function(info) {
    var form = Event.findElement(info, "form");
    var id = null;
    var width = null;
    $A(Form.getElements(form)).each(function(i) {
        var name = i.name;
        var value = Form.Element.getValue(i);
        if (name == "id") {
          id = value;
        } else if (name == "width") {
          width = value;
        }
      });
    var url = nxthemesBasePath + "/nxthemes-editor/update_element_width"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             'id': id,
             'width': width
         },
         onSuccess: function(r) {
        	 NXThemesEditor.highlightSaveButton();
             NXThemesEditor.refreshCanvas();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });       
};

NXThemesEditor.updateElementProperties = function(info) {
    var form = Event.findElement(info, "form");
    var id = null;
    var propertyMap = $H();
    $A(Form.getElements(form)).each(function(i) {
        var name = i.name;
        var value = Form.Element.getValue(i);
        if (name == "id") {
          id = value;
        } else {
          propertyMap.set(i.name, value);
        }
    });
    var url = nxthemesBasePath + "/nxthemes-editor/update_element_properties"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             id: id,
             property_map: propertyMap.toJSON()
         },
         onSuccess: function(r) {
        	 NXThemesEditor.highlightSaveButton();
             NXThemes.getViewById("element properties").refresh();
             NXThemesEditor.writeMessage("Properties updated.");
         },
         onFailure: function(r) {
             NXThemes.getViewById("element properties").refresh();
             NXThemesEditor.writeMessage("Properties could not be updated.");
         }         
    });
};

NXThemesEditor.updateElementWidget = function(info) {
    var form = Event.findElement(info, "form");
    var id = null;
    var viewName = "";
    $A(Form.getElements(form)).each(function(i) {
        var name = i.name;
        var value = Form.Element.getValue(i);
        if (name == "id") {
          id = value;
        } else if (name == "viewName") {
          viewName = value;
        }
    });
    if (!viewName) {
        return;
    }
    var url = nxthemesBasePath + "/nxthemes-editor/update_element_widget"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             'id': id,
             'view_name': viewName,
         },
         onSuccess: function(r) {
        	 NXThemesEditor.highlightSaveButton();
             NXThemesEditor.writeMessage("Widget changed.");
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};


NXThemesEditor.updateElementDescription = function(info) {
    var form = Event.findElement(info, "form");
    var id = null;
    var description = "";
    $A(Form.getElements(form)).each(function(i) {
        var name = i.name;
        var value = Form.Element.getValue(i);
        if (name == "id") {
          id = value;
        } else if (name == "description") {
          description = value;
        }
    });

    if (!description) {
      return;
    }
    var url = nxthemesBasePath + "/nxthemes-editor/update_element_description"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             'id': id,
             'description': description
         },
         onSuccess: function(r) {
        	 NXThemesEditor.highlightSaveButton();
             NXThemesEditor.writeMessage("Description changed.");
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });         
};


NXThemesEditor.updateElementStyle = function() {
    var form = $('nxthemesElementStyle');
    var id, path, viewName;
    var propertyMap = $H();
    $A(Form.getElements(form)).each(function(i) {
        var name = i.name;
        var value = Form.Element.getValue(i);
        if (name == "id") {
          id = value;
        } else if (name == "path") {
          path = value;
        } else if (name == "viewName") {
          viewName = value;
        } else if (name.indexOf('property:') === 0) {
          propertyMap.set(name.substr(9), value);
        }
    });
    
    var url = nxthemesBasePath + "/nxthemes-editor/update_element_style"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             id: id,
             view_name: viewName,
             path: path,
             property_map: propertyMap.toJSON()
         },
         onSuccess: function(r) {
        	 NXThemesEditor.highlightSaveButton();
             NXThemesStyleEditor.refreshCssPreview();
             NXThemesEditor.writeMessage("Style updated.");
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};

NXThemesEditor.updateElementStyleCss = function() {
    var form = $('nxthemesElementStyleCSS');
    var cssSource, viewName, id;
    Form.getElements(form).each(function(i) {
        var name = i.name;
        var value = $F(i);
        if (name == "cssSource") {
          cssSource = value;
        } else if (name == "id") {
          id = value;
        } else if (name == "viewName") {
          viewName = value;
        }
    });
    var url = nxthemesBasePath + "/nxthemes-editor/update_element_style_css"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             id: id,
             view_name: viewName,
             css_source: cssSource
         },
         onSuccess: function(r) {
        	 NXThemesEditor.highlightSaveButton();
             NXThemesStyleEditor.refreshCssPreview();
             NXThemesEditor.writeMessage("Style updated.");
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};

NXThemesEditor.setElementVisibility = function(info) {
    var form = Event.findElement(info, "form");
    var id = null;
    var alwaysVisible = false;
    var perspectives = [];
    Form.getElements(form).each(function(i) {
        var name = i.name;
        var value = $F(i);
        if (name == "id") {
          id = value;
        } else if (name == "alwaysVisible") {
          alwaysVisible = value ? true : false;
        } else if (name == "perspectives") {
          perspectives = $F("perspectives");
        }
    });
    var url = nxthemesBasePath + "/nxthemes-editor/update_element_visibility"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             'id': id,
             'always_visible': alwaysVisible,
             'perspectives': perspectives
         },
         onSuccess: function(r) {
        	 NXThemesEditor.highlightSaveButton();
             NXThemes.getViewById("element visibility").refresh();
             NXThemesEditor.writeMessage("Visibility changed.");
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};

NXThemesEditor.setElementWidget = function(info) {
    var id =  info.target.getAttribute('id');
    if (id === null) {
      return;
    }
    var viewName = info.options.choice;
    if (!viewName) {
        return;
    }
    var url = nxthemesBasePath + "/nxthemes-editor/update_element_widget"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             'id': id,
             'view_name': viewName
         },
         onSuccess: function(r) {
             NXThemesEditor.refreshCanvas();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};

NXThemesEditor.copyElement = function(info) {
    var id =  info.target.getAttribute('id');
    var url = nxthemesBasePath + "/nxthemes-editor/copy_element"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             id: id
         },
         onSuccess: function(r) {
             NXThemesEditor.writeMessage("Element copied.");
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};

NXThemesEditor.splitElement = function(info) {
    var id =  info.target.getAttribute('id');
    var url = nxthemesBasePath + "/nxthemes-editor/split_element"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             id: id
         },
         onSuccess: function(r) {
        	 NXThemesEditor.highlightSaveButton();
             NXThemesEditor.refreshCanvas();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });       
};

NXThemesEditor.setElementPadding = function(info) {
    var id = info.target.getAttribute('id');
    var url = nxthemesBasePath + "/nxthemes-editor/select_element"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             'id': id
         },
         onSuccess: function(r) {
             NXThemes.getControllerById("editor perspectives").switchTo("edit padding");
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};

NXThemesEditor.updateElementPadding = function(info) {
    var form = Event.findElement(info, "form");
    var id = null;
    var propertyMap = $H();
    $A(Form.getElements(form)).each(function(i) {
        var name = i.name;
        var value = Form.Element.getValue(i);
        propertyMap.set(i.name, value);
    });    
    var url = nxthemesBasePath + "/nxthemes-editor/update_element_layout"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             property_map: propertyMap.toJSON()
         },
         onSuccess: function(r) {
        	 NXThemesEditor.highlightSaveButton();
             NXThemesEditor.refreshCanvas();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};

NXThemesEditor.alignElement = function(info) {
    var target = info.target;
    var id =  target.getAttribute('id');
    if (id === null) {
      return;
    }
    var position = info.options.choice;
    var url = nxthemesBasePath + "/nxthemes-editor/align_element"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             id: id,
             position: position
         },
         onSuccess: function(r) {
        	 NXThemesEditor.highlightSaveButton();
             NXThemesEditor.refreshCanvas();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });     
};

NXThemesEditor.duplicateElement = function(info) {
    var id =  info.target.getAttribute('id');
    var url = nxthemesBasePath + "/nxthemes-editor/duplicate_element"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             id: id
         },
         onSuccess: function(r) {
        	 NXThemesEditor.highlightSaveButton();
             NXThemes.getViewById("style css").hide();
             NXThemes.getViewById("style css").show();
             NXThemesEditor.refreshCanvas();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }
    });       
};

NXThemesEditor.pasteElement = function(info) {
    var destId =  info.target.getAttribute('id');
    var url = nxthemesBasePath + "/nxthemes-editor/paste_element"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             dest_id: destId
         },
         onSuccess: function(r) {
        	 NXThemesEditor.highlightSaveButton();
             NXThemes.getViewById("style css").hide();
             NXThemes.getViewById("style css").show();
             NXThemesEditor.refreshCanvas();
             NXThemesEditor.writeMessage("Element pasted.");
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }  
    });
};

NXThemesEditor.deleteElement = function(info) {
    var id =  info.target.getAttribute('id');
    var url = nxthemesBasePath + "/nxthemes-editor/delete_element";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             id: id
         },
         onSuccess: function(r) {
        	 NXThemesEditor.highlightSaveButton();
             NXThemesEditor.refreshCanvas();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         },
         
    });
};

NXThemesEditor.selectTheme = function(name, viewId) {
    if (name !== null) {
        NXThemes.setCookie("nxthemes.theme", name);
        NXThemes.getViewById(viewId).refresh();
    }
};

NXThemesEditor.selectPerspective = function(info) {
    var form = Event.findElement(info, "form");
    var perspective = Form.findFirstElement(form).getValue();
    NXThemes.setCookie("nxthemes.perspective", perspective);
    NXThemesEditor.refreshCanvas();
};

NXThemesEditor.switchTheme = function(info) {
    var form = Event.findElement(info, "form");
    var name = Form.findFirstElement(form).getValue();
    NXThemesEditor.selectTheme(name, "theme selector");
    NXThemes.getViewById("page selector").refresh();
    NXThemesEditor.refreshCanvas();
};

NXThemesEditor.switchPage = function(info) {
    var target = Event.element(info);
    var name = target.getAttribute("name");
    if (name !== null) {
        NXThemesEditor.selectTheme(name, "page selector");
        NXThemesEditor.refreshCanvas();
    }
};

NXThemesEditor.addTheme = function() {
    var name = prompt("Enter a theme name:", "");
    if (name === "") {
        window.alert("Theme names cannot be empty.");
        return "";
    }
    if (!name.match(/^([a-z]|[a-z][a-z0-9_\-]*?[a-z0-9])$/)) {
        window.alert("Theme names may only contain lower-case alpha-numeric characters, digits, underscores and dashes");
        return "";
    }
    var url = nxthemesBasePath + "/nxthemes-editor/add_theme";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             name: name
         },
         onSuccess: function(r) {
             var text = r.responseText;
             NXThemesEditor.selectTheme(text, "theme selector");
             NXThemes.getViewById("theme manager").refresh();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};

NXThemesEditor.addPage = function(themeName) {
    var name = prompt("Please enter a page name:", "");
    if (name === "") {
        window.alert("Page names cannot be empty.");
        return "";
    }
    if (!name.match(/^([a-z]|[a-z][a-z0-9_\-]*?[a-z0-9])$/)) {
        window.alert("Page names may only contain lower-case alpha-numeric characters, digits, underscores and dashes.");
        return "";
    }
    var url = nxthemesBasePath + "/nxthemes-editor/add_page";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             path: themeName + '/' + name
         },
         onSuccess: function(r) {
             var text = r.responseText;
             NXThemesEditor.highlightSaveButton();
             NXThemesEditor.selectTheme(text, "page selector");
             NXThemesEditor.refreshCanvas();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }            
    });    
};

NXThemesEditor.switchToCanvas = function() {
    NXThemes.getControllerById('editor buttons').select('edit canvas');
    NXThemes.getControllerById('editor perspectives').switchTo('edit canvas');
};

NXThemesEditor.editCanvas = function() {
	NXThemesEditor.switchToCanvas();
    NXThemesEditor.refreshCanvas();
}

NXThemesEditor.addPreset = function(themeName, category, view_id) {
    var name = prompt("Please enter a preset name:", "");
    if (name === "") {
        window.alert("Preset names cannot be empty.");
        return "";
    }
    if (!NXThemesEditor.isLowerCaseOrSpace(name)) {
        window.alert("Preset names may only contain lower case characters and spaces.");
        return "";
    }
    var url = nxthemesBasePath + "/nxthemes-editor/add_preset";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             theme_name: themeName,
             preset_name: name,
             category: category
         },
         onSuccess: function(r) {
        	 NXThemesEditor.highlightSaveButton();
             NXThemes.getViewById(view_id).refresh();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }
    });     
};


NXThemesEditor.editPreset = function(themeName, presetName, value, view_id) {
    var value = prompt("Enter a CSS value:", value);
    if (!value) {
  	  return;
    }
    var url = nxthemesBasePath + "/nxthemes-editor/edit_preset";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             theme_name: themeName,
             preset_name: presetName,
             value: value
         },
         onSuccess: function(r) {
        	 NXThemesEditor.highlightSaveButton();
             NXThemes.getViewById(view_id).refresh();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });      
};
 
NXThemesEditor.addSection = function(info) {
    var target = Event.element(info);
    var id = target.getAttribute('sectionid') || target.getAttribute('pageid');
    var url = nxthemesBasePath + "/nxthemes-editor/insert_section_after";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             id: id
         },         
         onSuccess: function(r) {
        	 NXThemesEditor.highlightSaveButton();
             NXThemesEditor.refreshCanvas();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};

NXThemesEditor.alignSection = function(info, position) {
    var target = Event.element(info);
    var id = target.getAttribute('sectionid');;
    var url = nxthemesBasePath + "/nxthemes-editor/align_element"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             id: id,
             position: position
         }, 
         onSuccess: function(r) {
        	 NXThemesEditor.highlightSaveButton();
             NXThemesEditor.refreshCanvas();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    }); 
};

NXThemesEditor.alignSectionLeft = function(info) {
    NXThemesEditor.alignSection(info, "left");
}

NXThemesEditor.alignSectionCenter = function(info) {
    NXThemesEditor.alignSection(info, "center");
}

NXThemesEditor.alignSectionRight = function(info) {
    NXThemesEditor.alignSection(info, "right");
}


NXThemesEditor.setAreaStyle = function(info) {
    var target = Event.element(info);
    var property = target.getAttribute('name');
    NXThemesEditor.currentProperty = property;
    var area = NXThemes.Canvas.getFirstParentNodeWithAnId(target);
    if (area !== null) {
        var id = area.getAttribute("id");
        if (id !== null) {
          NXThemesEditor.selectedElement = id;
        }
    }
    var category = null;
    if (property == 'background') {
      category = 'background';
    } else if (property == 'border-top') {
      category = 'border';
    } else if (property == 'border-left') {
      category = 'border';
    } else if (property == 'border-bottom') {
      category = 'border';
    } else if (property == 'border-right') {
      category = 'border';
    }
    var url = nxthemesBasePath + "/nxthemes-editor/select_style_category"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             category: category
         },          
         onSuccess: function(r) {
        	 NXThemesEditor.highlightSaveButton();
             NXThemes.getControllerById('area style perspectives').switchTo('style chooser');
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};

NXThemesEditor.updateAreaStyle = function(value) {
  NXThemesEditor.closeAreaStyleChooser();
  var element_id = NXThemesEditor.selectedElement;
  if (element_id === null) {
    return;
  }
  var property = NXThemesEditor.currentProperty;
  if (property !== null) {
      if (value == null) {
          value = '';
      }
      var url = nxthemesBasePath + "/nxthemes-editor/assign_style_property"; 
      new Ajax.Request(url, {
         method: 'post',
         parameters: {
             element_id: element_id,
             property: property,
             value: value
         }, 
         onSuccess: function(r) {
        	 NXThemesEditor.highlightSaveButton();
             NXThemes.getViewById("style css").hide();
             NXThemes.getViewById("style css").show();
             NXThemesEditor.refreshCanvas();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
      });
  }
};

NXThemesEditor.setPresetGroup = function(select) {
    var group = select.value;
    var url = nxthemesBasePath + "/nxthemes-editor/select_preset_group"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             group: group
         }, 
         onSuccess: function(r) {
             NXThemes.getViewById("area style chooser").refresh();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};

NXThemesEditor.closeAreaStyleChooser = function() {
    NXThemes.getControllerById('area style perspectives').switchTo('default');
};

NXThemesEditor.refresh = function() {
  var url = nxthemesBasePath + "/nxthemes-editor/expire_themes"; 
  new Ajax.Request(url, {
         method: 'post',
         onSuccess: function(r) {
             NXThemesEditor.refreshCanvas();
             NXThemesEditor.writeMessage("Canvas refreshed.");
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
  });
};

NXThemesEditor.exit = function() {
  NXThemes.expireCookie("nxthemes.theme");
  NXThemes.expireCookie("nxthemes.engine");
  NXThemes.expireCookie("nxthemes.mode");
  NXThemes.expireCookie("nxthemes.perspective");
  var url = nxthemesBasePath + "/nxthemes-editor/clear_selections"; 
  new Ajax.Request(url, {
         method: 'post',
         onSuccess: function(r) {
             window.location.reload();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
  });
};

NXThemesEditor.repairTheme = function(src) {
    var url = nxthemesBasePath + "/nxthemes-editor/repair_theme"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
    	     src: src
         },
         onSuccess: function(r) {
        	 NXThemesEditor.highlightSaveButton();
             NXThemes.getViewById("theme manager").refresh();
             NXThemesEditor.writeMessage("Theme cleaned up.");
         },
         onFailure: function(r) {
             NXThemes.getViewById("theme manager").refresh();
             var text = r.responseText;
             window.alert(text);
         }         
    });
};

NXThemesEditor.loadTheme = function(src) {
    var ok = confirm("Unsaved changes will be lost, are you sure?");
    if (!ok) {
        return;
    }
    var url = nxthemesBasePath + "/nxthemes-editor/load_theme"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             src: src
         },
         onSuccess: function(r) {
           NXThemesEditor.dehighlightSaveButton();
           NXThemes.getViewById("theme manager").refresh();
           NXThemesEditor.writeMessage("Theme loaded.");
         },
         onFailure: function(r) {
           NXThemes.getViewById("theme manager").refresh();
           var text = r.responseText;
           window.alert(text);
         }          
    });
};

NXThemesEditor.saveTheme = function(src, indent) {
    var url = nxthemesBasePath + "/nxthemes-editor/save_theme"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             src: src,
             indent: indent
         },
         onSuccess: function(r) {
           NXThemes.getViewById("theme manager").refresh();
           NXThemesEditor.writeMessage("Theme saved.");
         },
         onFailure: function(r) {
           NXThemes.getViewById("theme manager").refresh();
           var text = r.responseText;
           window.alert(text);
         }              
    });
};

NXThemesEditor.deleteTheme = function(src) {
    var ok = confirm("Deleting theme, are you sure?");
    if (!ok) {
        return;
    }
    var url = nxthemesBasePath + "/nxthemes-editor/delete_theme"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             src: src
         },
         onSuccess: function(r) {
           NXThemes.getViewById("theme manager").refresh();
           NXThemesEditor.writeMessage("Theme deleted.");
         },
         onFailure: function(r) {
           NXThemes.getViewById("theme manager").refresh();
           var text = r.responseText;
           window.alert(text);
         }              
    });
};

NXThemesEditor.saveChanges = function() {
    var url = nxthemesBasePath + "/nxthemes-editor/save_changes"; 
    new Ajax.Request(url, {
         method: 'post',
         onSuccess: function(r) {
           NXThemesEditor.writeMessage("Changes saved.");
           NXThemesEditor.dehighlightSaveButton();
         },
         onFailure: function(r) {
           var text = r.responseText;
           window.alert(text);
         }      
    });
};

NXThemesEditor.refreshCanvas = function() {
    NXThemes.getViewById("canvas area").refresh();
};


NXThemesEditor.StyleCss = Class.create();
NXThemesEditor.StyleCss.prototype = Object.extend(new NXThemes.View(), {

  show: function() {
    NXThemes.Canvas.addStyleSheet('nxthemes-css', '/nuxeo/nxthemes-css/?timestamp=' + new Date().getTime());
  },

  hide: function() {
    NXThemes.Canvas.removeStyleSheet('nxthemes-css');
  }

});

// widgets
NXThemes.registerWidgets({

  stylecss: function(def) {
    var widget = NXThemes.Canvas.createNode({
      tag: 'div'
    });
    return new NXThemesEditor.StyleCss(widget, def);
  }

});

// Initialization

NXThemes.addActions({
    'move element': NXThemesEditor.moveElement,
    'insert fragment': NXThemesEditor.insertFragment,
    'edit element': NXThemesEditor.editElement,
    'duplicate element': NXThemesEditor.duplicateElement,
    'update element properties': NXThemesEditor.updateElementProperties,
    'update element widget': NXThemesEditor.updateElementWidget,
    'update element style': NXThemesEditor.updateElementStyle,
    'update element style css': NXThemesEditor.updateElementStyleCss,
    'update element description': NXThemesEditor.updateElementDescription,
    'set element visibility': NXThemesEditor.setElementVisibility,
    'update element padding': NXThemesEditor.updateElementPadding,
    'copy element': NXThemesEditor.copyElement,
    'paste element': NXThemesEditor.pasteElement,
    'delete element': NXThemesEditor.deleteElement,
    'select perspective': NXThemesEditor.selectPerspective,
    'switch theme': NXThemesEditor.switchTheme,
    'switch page': NXThemesEditor.switchPage,
    'set canvas mode': NXThemesEditor.setCanvasMode,
    'set size': NXThemesEditor.setSize,
    'add section': NXThemesEditor.addSection,
    'align section left': NXThemesEditor.alignSectionLeft,
    'align section center': NXThemesEditor.alignSectionCenter,
    'align section right': NXThemesEditor.alignSectionRight,        
    'align element': NXThemesEditor.alignElement,
    'split element': NXThemesEditor.splitElement,
    'set element padding': NXThemesEditor.setElementPadding,
    'delete page': NXThemesEditor.deletePage,
    'set element widget': NXThemesEditor.setElementWidget,
    'set area style': NXThemesEditor.setAreaStyle,
    'change element style': NXThemesEditor.changeElementStyle,
    'cancel event': function(info) {Event.stop(info);}
});

// Filters
NXThemesEditor.cleanUpCanvas = function(html) {
    // remove inline xmlns="..."
    html = html.replace(/xmlns="(.*?)"/g, "");
    // remove inline onclick="..."
    html = html.replace(/onclick="(.*?)"/g, "");
    return html;
};

NXThemes.registerFilters({
    'clean up canvas': NXThemesEditor.cleanUpCanvas
});


//PRESET MANAGER

if (typeof NXThemesPresetManager == "undefined") {
    NXThemesPresetManager = {
    }
}

NXThemesPresetManager.setEditMode = function(mode) {
    var url = nxthemesBasePath + "/nxthemes-editor/select_preset_manager_mode"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             mode: mode
         },
         onSuccess: function(r) {
        	 NXThemes.getViewById("preset manager").refresh();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};

NXThemesPresetManager.selectPresetGroup = function(group) {
    var url = nxthemesBasePath + "/nxthemes-editor/select_preset_group"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             group: group
         },
         onSuccess: function(r) {
        	 NXThemes.getViewById("preset manager").refresh();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};

NXThemesPresetManager.editPreset = function(info) {
  var target = Event.element(info);
  var model = info.model;
  var data = model.getData();
  var themeName = data.get('theme_name');
  var presetName = data.get('name');
  var value = data.get('value');
  value = prompt("Enter a CSS value:", value);
  if (!value) {
	  return;
  }
  var url = nxthemesBasePath + "/nxthemes-editor/edit_preset";
  new Ajax.Request(url, {
       method: 'post',
       parameters: {
           theme_name: themeName,
           preset_name: presetName,
           value: value
       },
       onSuccess: function(r) {
    	   NXThemesEditor.highlightSaveButton();
           NXThemes.getViewById("preset manager").refresh();
       },
       onFailure: function(r) {
           var text = r.responseText;
           window.alert(text);
       }       
  });          
}

NXThemesPresetManager.renamePreset = function(info) {
  var target = Event.element(info);
  var model = info.model;
  var data = model.getData();
  var themeName = data.get('theme_name');
  var oldName = data.get('name');
  var newName = prompt("Enter a preset name:", oldName);
  var url = nxthemesBasePath + "/nxthemes-editor/rename_preset";
  new Ajax.Request(url, {
       method: 'post',
       parameters: {
           theme_name: themeName,
           old_name: oldName,
           new_name: newName
       },
       onSuccess: function(r) {
    	   NXThemesEditor.highlightSaveButton();
           NXThemes.getViewById("preset manager").refresh();
       },
       onFailure: function(r) {
           var text = r.responseText;
           window.alert(text);
       }
  });          
}

NXThemesPresetManager.copyPreset = function(info) {
  var target = Event.element(info);
  var model = info.model;
  var data = model.getData();
  var id = data.get('id');
  var url = nxthemesBasePath + "/nxthemes-editor/copy_preset"; 
  new Ajax.Request(url, {
       method: 'post',
       parameters: {
           id: id
       },
       onSuccess: function(r) {
           NXThemesEditor.writeMessage("Preset copied.");
       },
       onFailure: function(r) {
           var text = r.responseText;
           window.alert(text);
       }       
  });    
}

NXThemesPresetManager.pastePreset = function(info) {
  var target = Event.element(info);
  var model = info.model;
  var data = model.getData();
  var themeName = data.get('theme_name');
  var presetName = prompt("Enter a preset name:");
  var url = nxthemesBasePath + "/nxthemes-editor/paste_preset"; 
  new Ajax.Request(url, {
       method: 'post',
       parameters: {
           theme_name: themeName,
           preset_name: presetName
       },
       onSuccess: function(r) {
    	   NXThemesEditor.highlightSaveButton();
           NXThemes.getViewById("preset manager").refresh();
       },
       onFailure: function(r) {
           var text = r.responseText;
           window.alert(text);
       }  
  });    
}

NXThemesPresetManager.deletePreset = function(info) {
  var target = Event.element(info);
  var model = info.model;
  var data = model.getData();
  var themeName = data.get('theme_name');
  var presetName = data.get('name');
  var url = nxthemesBasePath + "/nxthemes-editor/delete_preset"; 
  new Ajax.Request(url, {
       method: 'post',
       parameters: {
           theme_name: themeName,
           preset_name: presetName
       },
       onSuccess: function(r) {
    	   NXThemesEditor.highlightSaveButton();
           NXThemes.getViewById("preset manager").refresh();
       },
       onFailure: function(r) {
           var text = r.responseText;
           window.alert(text);
       }  
  });    
}

NXThemesPresetManager.setPresetCategory = function(info) {
	var target = Event.element(info);
	var model = info.model;
	var data = model.getData();
    var category = info.options.choice;
    if (!category) {
        return;
    }
    var themeName = data.get('theme_name');
    var presetName = data.get('name');
    var url = nxthemesBasePath + "/nxthemes-editor/set_preset_category";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             theme_name: themeName,
             preset_name: presetName,
             category: category
         },
         onSuccess: function(r) {
        	 NXThemesEditor.highlightSaveButton();
             NXThemes.getViewById("preset manager").refresh();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }
    });         
};

NXThemesPresetManager.addMissingPreset = function(themeName, presetName) {
    var presetValue = prompt("Enter a CSS value:", "");
    if (!presetValue) {
        return;
    } 
    var url = nxthemesBasePath + "/nxthemes-editor/add_preset";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             theme_name: themeName,
             preset_name: presetName,
             category: "",
             value: presetValue
         },
         onSuccess: function(r) {
        	 NXThemesEditor.highlightSaveButton();
             NXThemes.getViewById("preset manager").refresh();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }
    });     
    
};

NXThemesPresetManager.convertValueToPreset = function(themeName, category, presetValue) {
    var presetName = prompt("Enter a preset name:", "");
    if (!presetName) {
        return;
    } 
    var url = nxthemesBasePath + "/nxthemes-editor/convert_to_preset";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             theme_name: themeName,
             preset_name: presetName,
             category: category,
             value: presetValue
         },
         onSuccess: function(r) {
        	 NXThemesEditor.highlightSaveButton();
             NXThemes.getViewById("preset manager").refresh();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }
    });     
    
};

NXThemes.addActions({
    'edit preset': NXThemesPresetManager.editPreset,
    'set preset category': NXThemesPresetManager.setPresetCategory,
    'rename preset': NXThemesPresetManager.renamePreset,    
    'copy preset': NXThemesPresetManager.copyPreset,
    'paste preset': NXThemesPresetManager.pastePreset,
    'delete preset': NXThemesPresetManager.deletePreset
});


// STYLE EDITOR

if (typeof NXThemesStyleEditor == "undefined") {
    NXThemesStyleEditor = {
        'currentProperty': null
    };
}

NXThemesStyleEditor.refreshPreview = function() {
    var previewArea = document.getElementById('stylePreviewArea');
    var element  = previewArea.getAttribute('element');
    NXThemesStyleEditor.renderElement(element, previewArea);
};

NXThemesStyleEditor.refreshCssPreview = function() {
    var url = nxthemesBasePath + "/nxthemes-editor/render_css_preview"; 
    new Ajax.Request(url, {
         method: 'get',
         onSuccess: function(r) {
           var text = r.responseText;
           $('previewCss').innerHTML = text;
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};

NXThemesStyleEditor.closeStylePicker = function() {
    NXThemes.getControllerById('style editor perspectives').switchTo('style properties');
};

NXThemesStyleEditor.renderElement = function(id, area) {
    // render an element inside an area
    var options = {
      method: 'get',
      onSuccess: function(req) {
        var temp = document.createElement("div");
        temp.innerHTML = req.responseText;
        var elementList = $(temp).select('#' + id);
        if (elementList.length > 0) {
          var element = elementList[0];
          var html = element.innerHTML;
          // remove inline xmlns="..."
          html = html.replace(/xmlns="(.*?)"/g, "");
          // remove inline onclick="..."
          html = html.replace(/onclick="(.*?)"/g, "");
          area.innerHTML = html;
        }
      }
    };
    var url = window.location.href;
    var i = url.indexOf('?');
    var query_params = $H({'engine': 'fragments-only'});
    if (i > 0) {
      var query_string = url.substr(i+1);
      query_params = query_params.update($H(query_string.toQueryParams()));
    }
    url = url.substr(0, i) + '?' + query_params.toQueryString();
    new Ajax.Request(url, options);
};

NXThemesStyleEditor.chooseStyleSelector = function(select) {
  var value = select.value;
  if (value === '') {
    value = null;
  }
  NXThemesStyleEditor.setStyleSelector(select.value);
};

NXThemesStyleEditor.setPresetGroup = function(select) {
    var group = select.value;
    var url = nxthemesBasePath + "/nxthemes-editor/select_preset_group"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             group: group
         },
         onSuccess: function(r) {
           NXThemesStyleEditor.refreshStylePicker();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};

NXThemesStyleEditor.setStyleSelector = function(selector) {
    var url = nxthemesBasePath + "/nxthemes-editor/select_style_selector"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             selector: selector
         },
         onSuccess: function(r) {
        	 NXThemesStyleEditor.setStyleEditMode("form");
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};

NXThemesStyleEditor.createStyle = function() {
    var url = nxthemesBasePath + "/nxthemes-editor/create_style"; 
    new Ajax.Request(url, {
         method: 'post',
         onSuccess: function(r) {
    	     NXThemesEditor.highlightSaveButton();
             NXThemes.getViewById("element style").refresh();
             NXThemesEditor.writeMessage("New style created.");
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};

NXThemesStyleEditor.getSelectorInContext = function(element, context) {
  if (element == context) {
    return "";
  }
  var i, node, tag, name, selector, classnames;
  node = element;

  // If no context is passed, use the document itself.
  if (!context) {
    context = document;
  }

  // start from the first contained element
  context = context.childNodes.item(0);

  var selectors = new Array();
  while (node) {
    if (node == context) {
      break;
    }
    tag = node.tagName;
    if (!tag) {
      break;
    }
    tag = tag.toLowerCase();
    classnames = node.className;
    name = '';
    if (classnames) {
      name = classnames.replace(' ', '.');
    }
    selector = name ? tag + '.' + name : tag;
    selectors.push(selector);
    node = node.parentNode;
  }

  // reverse the array
  selectors.reverse();
  return selectors.join(" ");
};

NXThemesStyleEditor.refreshEditor = function() {
  NXThemes.getViewById("element style").refresh();
};

NXThemesStyleEditor.selectTag = function(info) {
    var target = info.target;
    Event.stop(info);
    var area = $('stylePreviewArea');
    var selector = NXThemesStyleEditor.getSelectorInContext(target, area);
    NXThemesStyleEditor.setStyleSelector(selector);
    return false;
};

NXThemesStyleEditor.setCurrentStyleLayer = function(uid) {
    var url = nxthemesBasePath + "/nxthemes-editor/select_style_layer"; 
      new Ajax.Request(url, {
         method: 'post',
         parameters: {
             uid: uid
         },
         onSuccess: function(r) {
             NXThemes.getControllerById('style editor perspectives').switchTo('default');
             NXThemes.getViewById("element style").refresh();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
      });
};

NXThemesStyleEditor.pickPropertyValue = function(info) {
    var target = info.target;
    var category = target.getAttribute('category');
    NXThemesStyleEditor.currentProperty = target.getAttribute('property');
    var url = nxthemesBasePath + "/nxthemes-editor/select_style_category"; 
      new Ajax.Request(url, {
         method: 'post',
         parameters: {
             category: category
         },
         onSuccess: function(r) {
           NXThemes.getControllerById('style editor perspectives').switchTo('style picker');
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
      });
};

NXThemesStyleEditor.setStyleEditMode = function(mode, fromMode) {
    if (fromMode == 'form') {
      NXThemesEditor.updateElementStyle();
    }
    if (fromMode == 'css') {
      NXThemesEditor.updateElementStyleCss();
    }
    var url = nxthemesBasePath + "/nxthemes-editor/select_style_edit_mode"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             mode: mode
         },
         onSuccess: function(req) {
           NXThemes.getControllerById("style editor perspectives").switchTo("style properties");
           NXThemes.getViewById("style properties").refresh();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};

NXThemesStyleEditor.setStylePropertyCategory = function(category) {
    var url = nxthemesBasePath + "/nxthemes-editor/select_style_property_category"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             category: category
         },
         onSuccess: function(req) {
           NXThemes.getViewById("style properties").refresh();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};

NXThemesStyleEditor.makeElementUseNamedStyle = function(select) {
    var value = select.value;
    if (value === '') {
        value = null;
    }
    var form = $(select).up("form");
    var id = form.getAttribute("element");
    var themeName = form.getAttribute("currentThemeName");
    var styleName = value;
    var url = nxthemesBasePath + "/nxthemes-editor/make_element_use_named_style";
    var parameters = {
        id: id,
        theme_name: themeName
    }    
    if (styleName) {
        parameters['style_name'] = styleName;
    }
    new Ajax.Request(url, {
         method: 'post',
         parameters: parameters,
         onSuccess: function(req) {
    	     NXThemesEditor.highlightSaveButton();
             NXThemes.getViewById("element style").refresh();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};

NXThemesStyleEditor.createNamedStyle = function(id, currentThemeName) {
    var styleName = prompt("Please enter a style name:", "");
    if (styleName === null) {
        return;
    }
    if (styleName === "") {
        window.alert("Style names cannot be empty.");
        return;
    }
    var url = nxthemesBasePath + "/nxthemes-editor/create_named_style"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             id: id,
             style_name: styleName,
             theme_name: currentThemeName
         },
         onSuccess: function(req) {
        	 NXThemesEditor.highlightSaveButton();
             NXThemes.getViewById("element style").refresh();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};

NXThemesStyleEditor.deleteNamedStyle = function(id, currentThemeName, styleName) {
    var ok = confirm("Deleting style, are you sure?");
    if (!ok) {
        return;
    }
    var url = nxthemesBasePath + "/nxthemes-editor/delete_named_style"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             id: id,
             style_name: styleName,
             theme_name: currentThemeName
         },
         onSuccess: function(r) {
        	 NXThemesEditor.highlightSaveButton();
             NXThemes.getViewById("element style").refresh();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }
    });
};

NXThemesStyleEditor.updateFormField = function(value) {
  var currentProperty = NXThemesStyleEditor.currentProperty;
  if (currentProperty !== null) {
    var propertyName = 'property:' + currentProperty;
    var inputs = Form.getInputs('nxthemesElementStyle', null, propertyName);
    if (inputs !== null) {
      inputs[0].value = value;
    }
  }
  NXThemesStyleEditor.closeStylePicker();
};

NXThemesStyleEditor.refreshStylePicker = function() {
    NXThemes.getViewById("style picker").refresh();
};


// widgets
NXThemes.registerWidgets({

  stylepreview: function(def) {
    var widget = NXThemes.Canvas.createNode({
      tag: 'div',
      classes: ['nxthemesStylePreview']
    });
    return new NXThemesStyleEditor.StylePreview(widget, def);
  }

});

NXThemesStyleEditor.StylePreview = Class.create();
NXThemesStyleEditor.StylePreview.prototype = Object.extend(new NXThemes.View(), {

  setup: function() {
    NXThemesStyleEditor.refreshPreview();
    NXThemesStyleEditor.refreshCssPreview();
  }

});

// actions
NXThemes.addActions({
  'select style tag': NXThemesStyleEditor.selectTag,
  'update style label': function(info) {
    var box = $('labelInfo');
    if (box === null) {
      return;
    }
    var x = Event.pointerX(info);
    var y = Event.pointerY(info);
    var target = info.target;
    var label= NXThemesStyleEditor.getSelectorInContext(target, $('stylePreviewArea'));
    if (!label) {
      label = '.';
    }
    box.innerHTML = label;
    box.setStyle({left: x+'px', top: y-45+'px'});
    box.show();
  },
  'hide style label': function(info) {
    $('labelInfo').hide();
  },
  'pick property value': NXThemesStyleEditor.pickPropertyValue
});


// STYLE MANAGER

if (typeof NXThemesStyleManager == "undefined") {
	NXThemesStyleManager = {
    };
}


NXThemesStyleManager.deleteUnusedStyleView = function(info) {
    var form = Event.findElement(info, "form");
    var themeName, styleUid, viewName;
    $A(Form.getElements(form)).each(function(i) {
        var name = i.name;
        var value = Form.Element.getValue(i);
        if (name == "style_uid") {
        	styleUid = value;
        } else if (name == "view_name") {
        	viewName = value;
        } else if (name == "theme_name") {
        	themeName = value;
        }
    });
    var url = nxthemesBasePath + "/nxthemes-editor/delete_style_view";
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             theme_name: themeName,
             view_name: viewName,
             style_uid: styleUid
         },
         onSuccess: function(r) {
        	 NXThemesEditor.highlightSaveButton();
             NXThemes.getViewById("style manager").refresh();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }
    });     
};

NXThemesStyleManager.setEditMode = function(mode) {
    var url = nxthemesBasePath + "/nxthemes-editor/select_style_manager_mode"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             mode: mode
         },
         onSuccess: function(req) {
        	 NXThemes.getViewById("style manager").refresh();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};

NXThemesStyleManager.updateNamedStyleCSS = function(form) {
	var style_uid = '';
	var css_source = '';
    $A(Form.getElements(form)).each(function(i) {
        var name = i.name;
        var value = Form.Element.getValue(i);
        if (name == "style_uid") {
          style_uid = value;
        } else if (name == "cssSource") {
          css_source = value;
        }
    });
    var url = nxthemesBasePath + "/nxthemes-editor/update_named_style_css"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             'style_uid': style_uid,
             'css_source': css_source
         },
         onSuccess: function(r) {
        	 NXThemesEditor.highlightSaveButton();
        	 NXThemes.getViewById("style manager").refresh();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });   
}

NXThemesStyleManager.selectNamedStyle = function(uid) {
    var url = nxthemesBasePath + "/nxthemes-editor/select_named_style"; 
      new Ajax.Request(url, {
         method: 'post',
         parameters: {
             uid: uid
         },
         onSuccess: function(r) {
             NXThemes.getViewById("style manager").refresh();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
      });
};

//actions
NXThemes.addActions({
  'delete unused style view': NXThemesStyleManager.deleteUnusedStyleView
});

// Fragment factory
if (typeof NXThemesFragmentFactory == "undefined") {
    NXThemesFragmentFactory = {
    }
}

NXThemesFragmentFactory.selectFragmentType = function(type) {
    var url = nxthemesBasePath + "/nxthemes-editor/select_fragment_type"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
             type: type
         },
         onSuccess: function(r) {
        	 NXThemes.getViewById("fragment factory").refresh();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};

NXThemesFragmentFactory.selectView = function(view) {
    var url = nxthemesBasePath + "/nxthemes-editor/select_fragment_view"; 
    new Ajax.Request(url, {
         method: 'post',
         parameters: {
    	     view: view
         },
         onSuccess: function(r) {
        	 NXThemes.getViewById("fragment factory").refresh();
         },
         onFailure: function(r) {
             var text = r.responseText;
             window.alert(text);
         }         
    });
};







