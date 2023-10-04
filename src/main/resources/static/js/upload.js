  htmx.on('#upForm', 'htmx:xhr:progress', function(evt) { 
          htmx.find('#progress').setAttribute('value', evt.detail.loaded/evt.detail.total * 100)
        });



