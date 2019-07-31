package com.oracle.fmwk8s.utility

import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.env.Database
@GrabResolver(name = 'fmw-virtual', root = 'http://artifactory-slc-prod1.oraclecorp.com/artifactory/fmw-virtual/')
@Grab('org.yaml:snakeyaml:1.24')
import org.yaml.snakeyaml.*

class YamlUtility {
    static pvInputsMap
    static domainInputsMap

    static generatePeristentVolumeInputsYaml(domainName, domainNamespace, nfsDomainPath, pvInputsYaml) {
        println("WORKING DIRECTORY : " + new File(".").getCanonicalPath())
        Map<Object, Object> map = readYaml(pvInputsYaml)
        println("Original Yaml")
        printMap(map)

        map.put("baseName", domainNamespace.toString())
        map.put("domainUID", domainName.toString())
        map.put("namespace", domainNamespace.toString())
        map.put("weblogicDomainStoragePath", nfsDomainPath.toString())
        map.put("weblogicDomainStorageReclaimPolicy", "Recycle")

        println("Modified Yaml")
        printMap(map)
        pvInputsMap = map

        writeYaml(map, pvInputsYaml)
    }

    static generateDomainInputsYaml(domainName, domainNamespace, domainInputsYaml) {
        Map<Object, Object> map = readYaml(domainInputsYaml)
        println("Original Yaml")
        printMap(map)

        map.put("adminPort", 7001)
        map.put("adminServerName", "admin-server")
        map.put("domainUID", domainName.toString())
        map.put("domainHome", "/shared/domains/" + domainName.toString())
        map.put("initialManagedServerReplicas", 2)
        map.put("managedServerPort", 8001)
        map.put("image", Common.productImage.toString())
        map.put("imagePullSecretName", Common.registrySecret.toString())
        map.put("weblogicCredentialsSecretName", domainName.toString() + "-weblogic-credentials")
        map.put("logHome", "/shared/logs/" + domainName.toString())
        map.put("exposeAdminT3Channel", true)
        map.put("t3PublicAddress", "fmwk8s.us.oracle.com")
        map.put("namespace", domainNamespace.toString())
        map.put("persistentVolumeClaimName", domainName.toString() + "-" + domainNamespace.toString() + "-pvc")
        map.put("rcuSchemaPrefix", domainName.toString())
        map.put("rcuDatabaseURL", Database.dbName.toString() + "." + domainNamespace.toString() + ":1521/" + Database.dbName.toString() + "pdb.us.oracle.com")
        map.put("rcuCredentialsSecret", domainName.toString() + "-rcu-credentials")

        println("Modified Yaml")
        printMap(map)
        domainInputsMap = map

        writeYaml(map, domainInputsYaml)
    }

    static readYaml(yamlFilePath) {
        def inputStream = new File(yamlFilePath).newInputStream()
        Yaml yamlReader = new Yaml()
        Map<Object, Object> map = yamlReader.load(inputStream)
        inputStream.close()

        return map
    }

    static writeYaml(map, yamlFilePath) {
        DumperOptions options = new DumperOptions()
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
        options.setPrettyFlow(true)

        FileWriter writer = new FileWriter(yamlFilePath)
        Yaml yamlWriter = new Yaml(options)
        yamlWriter.dump(map, writer)
    }

    static printMap(map) {
        map.each { k, v -> println "${k}:${v}" }
    }
}