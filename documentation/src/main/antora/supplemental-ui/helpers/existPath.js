'use strict';

function validateUrlInSite(url, siteData) {
  function searchInNavigation(items) {
    if (!items || !Array.isArray(items)) return false;

    return items.some(item => {
      if (item.url === url) return true;
      return item.items && searchInNavigation(item.items);
    });
  }

  return siteData.versions.some(version =>
    version.navigation &&
    version.navigation.some(nav => searchInNavigation(nav.items))
  );
}

module.exports = (path, siteData) => {
  // Check if path ends with "all-in-one" or exists in site navigation
  if (path && path.endsWith('all-in-one.html')) {
    return path;
  }

  return validateUrlInSite(path, siteData) ? path : null;
};
