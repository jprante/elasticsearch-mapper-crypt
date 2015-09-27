![SHA](https://github.com/jprante/elasticsearch-mapper-crypt/raw/master/src/site/resources/sha.jpg)

Photo taken from https://www.flickr.com/photos/byzantiumbooks/15129679930
by Bill Smith, Creative Commons Attribution 2.0 Generic https://creativecommons.org/licenses/by/2.0/

# Crypt field mapper plugin for Elasticsearch

The crypt mapper plugin can index the secure hash of a string field when indexing to
[Elasticsearch](http://github.com/elasticsearch/elasticsearch).

This is useful for storing and retrieving content that must be protected by
a cryptographic hash, like passwords.

Default is the SHA-256 algorithm. You can use any secure hash algorithm that is
supported by your JVM's `MessageDigest` class.

Important note: you must disable the '_source' field when using this plugin,
or your information will be stored in clear text.

Also note, the field content is transported in clear text to the indexing node. 
If you want a secure transmission, you have to use extensions that can protect 
Elasticsearch TCP/IP traffic.

It is not recommended to use MD5. See also http://www.win.tue.nl/hashclash/rogue-ca/

## Compatibility matrix

![Travis](https://travis-ci.org/jprante/elasticsearch-knapsack.png)

| Elasticsearch  |   Plugin       | Release date |
| -------------- | -------------- | ------------ |
| 2.0.0-beta2    | 2.0.0-beta2.1  | Sep 27, 2015 |

## Installation 1.x

    ./bin/plugin -install cryptmapper -url http://xbib.org/repository/org/xbib/elasticsearch/plugin/elasticsearch-mapper-crypt/1.7.2.0/elasticsearch-mapper-crypt-1.7.2.0-plugin.zip

## Installation 2.x

    ./bin/plugin install http://xbib.org/repository/org/xbib/elasticsearch/plugin/elasticsearch-mapper-crypt/2.0.0-beta2.0/elasticsearch-mapper-crypt-2.0.0-beta2.0-plugin.zip

Do not forget to restart the node after installation.

## Project docs

The Maven project site is available at [Github](http://jprante.github.io/elasticsearch-mapper-crypt)

# Example index mapping

    {
        "someType" : {
            "_source" : {
                "enabled": false
            },
            "properties" : {
                "someField": { 
                    "type" : "crypt"
                    "algo" : "SHA-256" 
                }
            }
        }
    }


# License

Crypt field mapper plugin for Elasticsearch

Copyright (C) 2015 Jörg Prante

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
