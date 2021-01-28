# dapla-user-access
[![Build Status](https://drone.prod-bip-ci.ssb.no/api/badges/statisticsnorway/dapla-user-access/status.svg)](https://drone.prod-bip-ci.ssb.no/statisticsnorway/dapla-user-access)


# REST API

## Get access
Determine if a given user can access a given dataset

* **URL**

    `/access/:userId?privilege=<privilege>&path=<path>&valuation=<valuation>&state=<state>`

* **Method**
    
    `GET`

* **URL Params**

    **Required:**
    
    `userId=[string]`

* **Query Params**

    **Required:**
    
    `privilege=["CREATE", "READ", "UPDATE" or "DELETE"]`
    
    `path=[string]`
     
    `valuation=["OPEN", "INTERNAL", "SHIELDED" or "SENSITIVE"]`
    
    `state=["RAW", "INPUT", "PROCESSED", "OUTPUT", "PRODUCT" or "OTHER"]`

* **Success Response:**
    * **Code:** `200 OK`
    
    OR
    
    * **Code:** `403 FORBIDDEN`

* **Error Response:**
    * **Code:** `500 INTERNAL SERVER ERROR`
    
* **Sample Call:**
    
    Note: The slashes in '`path=/ns/test`' are url-escaped 
    ```bash
    $ curl -i "http://localhost:8080/access/123?privilege=UPDATE&path=%2Fns%2Ftest&valuation=INTERNAL&state=RAW"
    HTTP/1.1 200 OK
    ``` 

## Show role
Return data about a single role

* **URL**

    `/role/:roleId`

* **Method**
    
    `GET`

* **URL Params**

    **Required:**
    
    `roleId=[string]`

* **Success Response:**
    * **Code:** `200 OK`
    
        **Content:** 
        ```
        {
          "roleId": "123",
          "privileges": [
            "CREATE",
            "UPDATE"
          ],
          "pathPrefixes": [
            "/ns/test"
          ],
          "maxValuation": "INTERNAL",
          "states": [
            "INPUT",
            "RAW"
          ]
        }
        ```

* **Error Response:**
    * **Code:** `404 NOT FOUND`
    
    OR
    
    * **Code:** `500 INTERNAL SERVER ERROR`
    
* **Sample Call:**

    ```
    $ curl -i http://localhost:8080/role/123
    HTTP/1.1 200 OK
  
    {"roleId":"123","privileges":["UPDATE","CREATE"],"pathPrefixes":["/ns/test"],"maxValuation":"INTERNAL","states":["RAW","INPUT"]}
    ``` 

## Create role
Create a new role

* **URL**

    `/role/:roleId`

* **Method**
    
    `PUT`

* **URL Params**

    **Required:**
    
    `roleId=[string]`
    
* **Data Params**
    
    ```
    {
      "roleId": "123",
      "privileges": [
        "CREATE",
        "UPDATE"
      ],
      "pathPrefixes": [
        "/ns/test"
      ],
      "maxValuation": "INTERNAL",
      "states": [
        "INPUT",
        "RAW"
      ]
    } 
    ```

* **Success Response:**
    * **Code:** `201 CREATED`
    
        **Headers:** `Location: /role/123`

* **Error Response:**
    * **Code:** `400 Bad Request`
    
        **Content:** `roleId in path must match that in body`
    
    OR
    
    * **Code:** `500 INTERNAL SERVER ERROR`
    
* **Sample Call:**

    ```bash
    $ curl -i -X PUT -d '{"roleId":"123","privileges":["CREATE","UPDATE"],"pathPrefixes":["/ns/test"],"maxValuation":"INTERNAL","states":["INPUT","RAW"]}' http://localhost:8080/role/123 
    HTTP/1.1 201 Created
    Location: /role/123
    ```

## Delete role
Delete a role

* **URL**

    `/role/:roleId`

* **Method**
    
    `DELETE`

* **URL Params**

    **Required:**
    
    `roleId=[string]`
    
* **Success Response:**
    * **Code:** `200 OK`

* **Error Response:**
    * **Code:** `500 INTERNAL SERVER ERROR`
    
* **Sample Call:**

    ```bash
    $ curl -i -X DELETE http://localhost:8080/role/123
    HTTP/1.1 200 OK
    ```

## Show user
Return data about a single user

* **URL**

    `/user/:userId`

* **Method**
    
    `GET`

* **URL Params**

    **Required:**
    
    `userId=[string]`

* **Success Response:**
    * **Code:** `200 OK`
    
        **Content:** `{ "userId" : "123", "roles" : [ "reader", "writer" ] }`

* **Error Response:**
    * **Code:** `404 NOT FOUND`
    
    OR
    
    * **Code:** `500 INTERNAL SERVER ERROR`
    
* **Sample Call:**

    ```bash
    $ curl -i http://localhost:8080/user/123
    HTTP/1.1 200 OK
    {"userId":"123","roles":["reader","writer"]}
    ```  


## Create User
Create a new user

* **URL**

    `/user/:userId`

* **Method**
    
    `PUT`

* **URL Params**

    **Required:**
    
    `userId=[string]`
    
* **Data Params**
    
    `{ "userId" : "123", "roles" : [ "reader", "writer" ] }`

* **Success Response:**
    * **Code:** `201 CREATED`
    
        **Headers:** `Location: /user/123`

* **Error Response:**
    * **Code:** `400 Bad Request`
    
        **Content:** `userId in path must match that in body`
    
    OR
    
    * **Code:** `500 INTERNAL SERVER ERROR`
    
* **Sample Call:**

    ```bash
    $ curl -i -X PUT -d '{"userId": "123", "roles": ["reader","writer"]}' http://localhost:8080/user/123
    HTTP/1.1 201 Created
    Location: /user/123
    ```
  
## Delete user
Delete a user

* **URL**

    `/user/:userId`

* **Method**
    
    `DELETE`

* **URL Params**

    **Required:**
    
    `userId=[string]`
    
* **Success Response:**
    * **Code:** `200 OK`

* **Error Response:**
    * **Code:** `500 INTERNAL SERVER ERROR`
    
* **Sample Call:**

    ```bash
    $ curl -i -X DELETE http://localhost:8080/user/123
    HTTP/1.1 200 OK
    ```