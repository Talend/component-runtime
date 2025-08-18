'use strict'

module.exports.register = function () {
  this.on('uiLoaded', ({ uiCatalog }) => {
    const handlebars = this.require('handlebars')

    handlebars.registerHelper('getAllPaths', function({ data }) {
      const { contentCatalog } = data.root

      if (!contentCatalog) return []

      const allPages = contentCatalog.getPages(({ out }) => {
        return out !== undefined
      })

      return allPages.map(page => ({
        url: page.pub.url,
        title: page.asciidoc ? page.asciidoc.doctitle : page.src.basename,
        component: page.src.component,
        version: page.src.version
      }))
    })

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