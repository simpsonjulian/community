###
Copyright (c) 2002-2012 "Neo Technology,"
Network Engine for Objects in Lund AB [http://neotechnology.com]

This file is part of Neo4j.

Neo4j is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
###

define(
  ['neo4j/webadmin/modules/databrowser/models/PropertyContainer'
   'ribcage/View'
   './propertyEditor'
   'lib/amd/jQuery'], 
  (PropertyContainer, View, propertyEditorTemplate, $) ->

    class PropertyContainerView extends View

      events :
        "keyup input.property-key"     : "keyChanged",
        "keyup input.property-value"   : "valueChanged",
        "change input.property-key"    : "keyChangeDone",
        "change input.property-value"  : "valueChangeDone",
        "click .delete-property"       : "deleteProperty",
        "click .add-property"          : "addProperty",
        "click .data-save-properties"  : "saveChanges"
        "click .data-delete-item"      : "deleteItem"

      initialize : (opts) =>
        @template = opts.template

      keyChanged : (ev) =>
        id = @getPropertyIdForElement(ev.target)
        if $(ev.target).val() != @propertyContainer.getProperty(id).getKey()
          @propertyContainer.setNotSaved()

      valueChanged : (ev) =>
        id = @getPropertyIdForElement(ev.target)
        if $(ev.target).val() != @propertyContainer.getProperty(id).getValueAsJSON()
          prop = @propertyContainer.setNotSaved()

      keyChangeDone : (ev) =>
        id = @getPropertyIdForElement(ev.target)
        @propertyContainer.setKey(id, $(ev.target).val())
        @saveChanges()
        @updateErrorMessages()

      valueChangeDone : (ev) =>    
        id = @getPropertyIdForElement(ev.target)
        el = $(ev.target)

        if @shouldBeConvertedToString(el.val())
          el.val('"' + el.val() + '"');
        
        @propertyContainer.setValue(id, el.val())
        @saveChanges()
        @updateErrorMessages()

      deleteProperty : (ev) =>
        id = @getPropertyIdForElement(ev.target)
        @propertyContainer.deleteProperty(id)
        @propertyContainer.save()

      addProperty : (ev) =>
        @propertyContainer.addProperty()

      saveChanges : (ev) =>
        @propertyContainer.save()

      deleteItem : (ev) =>
        if confirm "Are you sure?"
          @propertyContainer.getItem().remove().then () ->
            window.location = "#/data/search/0"
      
      updateErrorMessages : () =>
        for row in $("ul.property-row",@el)
          id =$(row).find("input.property-id").val()
          prop = @propertyContainer.getProperty(id)
          if prop
            keyErrorEl = $(row).find(".property-key-wrap .form-error")
            valueErrorEl = $(row).find(".property-value-wrap .form-error")
            if prop.hasKeyError() 
              keyErrorEl.html(prop.getKeyError())
              keyErrorEl.show()
            else keyErrorEl.hide()
            
            if prop.hasValueError() 
              valueErrorEl.html(prop.getValueError())
              valueErrorEl.show()
            else valueErrorEl.hide()
            
      updateSaveState : (ev) =>
        state = @propertyContainer.getSaveState()
        switch state
          when "saved" then @setSaveState "Saved", true
          when "notSaved" then @setSaveState "Save", false
          when "saving" then @setSaveState "Saving..", true
          when "cantSave" then @setSaveState "Save", true

      setSaveState : (text, disabled) =>
        button = $(".data-save-properties", @el)
        button.html(text)
        if disabled
          button.attr("disabled","disabled")
        else
          button.removeAttr("disabled")
        
      getPropertyIdForElement : (element) =>
        $(element).closest("ul").find("input.property-id").val()

      setData : (@propertyContainer) =>
        @unbind()
        @propertyContainer.bind "remove:property", @renderProperties
        @propertyContainer.bind "add:property", @renderProperties
        @propertyContainer.bind "change:status", @updateSaveState

      render : =>
        $(@el).html(@template(
          item : @propertyContainer
        ))
        @renderProperties()
        return this

      renderProperties : =>
        $(".properties",@el).html(propertyEditorTemplate(
          properties : @propertyContainer.get "propertyList"
        ))

        return this

      remove : =>
        @unbind()
        super()

      unbind : =>
        if @propertyContainer?
          @propertyContainer.unbind "remove:property", @renderProperties
          @propertyContainer.unbind "add:property", @renderProperties
          @propertyContainer.unbind "change:status", @updateSaveState

      stringRegex : /// ^ 
                    [^
                      (^(\d+)(‎\.(\d+))?$) # Not stuff that looks like numbers
                      (^\[(.*)\]$)        # Not stuff that looks like an array
                    ]
                    ///i
      shouldBeConvertedToString : (val) =>
        try 
          JSON.parse val
          return false
        catch e
          return @stringRegex.test(val)
)
