// https://stackoverflow.com/questions/2190801/passing-parameters-to-javascript-files

var CODE_LANGUAGE_TOGGLER = CODE_LANGUAGE_TOGGLER || (function () {
  var _window = {}
  var _document = {}
  var _availableConfigs = []

  return {
    init: function (window, document, availableConfigs) {
      _window = window
      _document = document
      _availableConfigs = availableConfigs
    },
    initBlocks: function () {
      var document = _document
      var blocks = document.getElementsByTagName('block')
      var article = document.getElementsByTagName('article')[0]
      for (var i = 0; i < blocks.length; ++i) {
        var block = blocks[i]
        article.insertBefore(block, null)
      }
      // Convert <div>...<block />content<block />...</div>
      // Into <div>...<block>content</block><block />...</div>
      blocks = document.getElementsByTagName('block')
      for (var i = 0; i < blocks.length; ++i) {
        var block = blocks[i]
        while (block.nextSibling && block.nextSibling.tagName !== 'BLOCK') {
          block.appendChild(block.nextSibling)
        }
      }
    },
    display: function (type, value) {
      var document = _document
      var container = document.getElementsByTagName('block')[0].parentNode
      container.className = 'display-' + type + '-' + value
    },
    loadFromHash: function () {
      // If we are coming to the page with a hash in it (i.e. from a search, for example), try to get
      // us as close as possible to the correct platform and dev os using the hashtag and block walk up.
      var availableConfigs = _availableConfigs
      var foundHash = false
      var window = _window
      if (window.location.hash !== '' && window.location.hash !== 'content') { // content is default
        var hashLinks = document.querySelectorAll('a.hash-link')
        for (var i = 0; i < hashLinks.length && !foundHash; ++i) {
          if (hashLinks[i].hash === window.location.hash) {
            var parent = hashLinks[i].parentElement
            while (parent) {
              if (parent.tagName === 'BLOCK') {
                var config = null

                for (var i = 0; i < availableConfigs.length; ++i) {
                  var availableConfig = availableConfigs[i]
                  if (parent.className.indexOf(availableConfig) > -1) {
                    config = availableConfig
                    break
                  }
                }

                if (config == null) {
                  break // assume we don't have anything.
                }

                display('configuration', config)
                foundHash = true
                break
              }
              parent = parent.parentElement
            }
          }
        }
      }

      // Do the default if there is no matching hash
      if (!foundHash) {
        display('configuration', availableConfigs[0])
      }
    }
  }
}())
