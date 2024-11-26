# Corese-server Changelog

## 4.6.1 - Elasticsearch

### Added
- Implementation of an elasticsearch connector
  - EdgeChangeListener implementation for Elasticsearch with associated tests
  - Model system created to transfer instances description according to models declared using the model defined by [Mnemotix](https://gitlab.com/mnemotix/synaptix/mnx-models/-/blob/aa8134f95b1db258b1678aab1030e70e6763925f/indexing-model/indexing-model.owl)
  - ElasticsearchControl class created to manually trigger elastic search indexing using call to `/elasticsearch`
- SPARQL endpoint settings centralized in the SPARQLEndpointCommons class
  
### Changed
- Code cleaning in accordance with the SonarLint rules