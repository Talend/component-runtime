'use strict'

module.exports.register = function () {
  this.on('uiLoaded', ({ uiCatalog }) => {
    const handlebars = this.require('handlebars')

    handlebars.registerHelper('validatePath', function(pathToCheck, { data }) {
      const { contentCatalog } = data.root

      if (!contentCatalog) {
          console.warn('Content catalog is not available.')
          return null
      }

      const allPages = contentCatalog.getPages(({ out }) => {
        return out !== undefined
      })

      const currentPage = allPages.some(page => page.pub.url === pathToCheck)
      if (!currentPage) {
          console.warn(`Path ${pathToCheck} does not exist`)
      }

      return currentPage ? pathToCheck : null
    })
  })
}