# linkedin-api
Tool to extract followed companies from LinkedIn, to use them as filter in job search

## Usage

`resources > single_files` extracted JSON from API calls loading the followed companies.

Sample API for first companies
```link
https://www.linkedin.com/voyager/api/graphql?variables=(profileUrn:urn:li:fsd_profile:ACoAACsPj_UBJmC5UVsOXrWeLh7u77aQ8GZSMV4,sectionType:interests,tabIndex:1)&queryId=voyagerIdentityDashProfileComponents.833eabb34214d6c6beae3e07db82ec41
```
Sample API for following companies (paginated) --> Copy All
```link
https://www.linkedin.com/voyager/api/graphql?includeWebMetadata=true&variables=(start:20,count:20,paginationToken:null,pagedListComponent:urn:li:fsd_profilePagedListComponent:(ACoAACsPj_UBJmC5UVsOXrWeLh7u77aQ8GZSMV4,INTERESTS_VIEW_DETAILS,urn:li:fsd_profileTabSection:COMPANIES_INTERESTS,NONE,en_US))&queryId=voyagerIdentityDashProfileComponents.a8c56b67fec7bf025d839566cbd89385
```

# TODO
- [X] Handle excluding companies
- [X] Create and handle `exclude_strict.csv`
- [X] Handle multiple files
- [X] Fix not found IDs