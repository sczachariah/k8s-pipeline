package com.oracle.fmwk8s.utility

import com.cloudbees.groovy.cps.NonCPS
import com.oracle.fmwk8s.common.Common
import com.oracle.fmwk8s.env.Database
@GrabResolver(name = 'fmw-virtual', root = 'http://artifactory-slc-prod1.oraclecorp.com/artifactory/fmw-virtual/')
@Grab('org.yaml:snakeyaml:1.24')
import org.yaml.snakeyaml.*

class YamlUtility implements Serializable {
    static pvInputsMap
    static domainInputsMap
    static Object[] domainYaml

    static void main(String[] args) {
        YamlUtility yamlUtility = new YamlUtility();
        yamlUtility.generateDomainYaml("oim", "domain.yaml")
    }

    static generatePeristentVolumeInputsYaml(script, domainName, domainNamespace, nfsDomainPath, pvInputsYamlFile) {
        Map<Object, Object> map = readYaml(script, pvInputsYamlFile)

        map.put("baseName", domainNamespace.toString())
        map.put("domainUID", domainName.toString())
        map.put("namespace", domainNamespace.toString())
        map.put("weblogicDomainStoragePath", nfsDomainPath.toString())
        map.put("weblogicDomainStorageReclaimPolicy", "Recycle")

        pvInputsMap = map
        writeYaml(script, map, pvInputsYamlFile)
    }

    static generateDomainInputsYaml(script, domainName, domainNamespace, domainInputsYamlFile) {
        Map<Object, Object> map = readYaml(script, domainInputsYamlFile)

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
        map.put("exposeAdminT3Channel", false)
        map.put("t3PublicAddress", "fmwk8s.us.oracle.com")
        map.put("namespace", domainNamespace.toString())
        map.put("persistentVolumeClaimName", domainName.toString() + "-" + domainNamespace.toString() + "-pvc")
        map.put("rcuSchemaPrefix", domainName.toString())
        map.put("rcuDatabaseURL", Database.dbName.toString() + "." + domainNamespace.toString() + ":" + Database.dbPort.toString() + "/" + Database.dbName.toString() + "pdb.us.oracle.com")
        map.put("rcuCredentialsSecret", domainName.toString() + "-rcu-credentials")

        domainInputsMap = map
        writeYaml(script, map, domainInputsYamlFile)
    }

    static generateDomainYaml(script, productId, domainYaml) {
        if ("${productId}" == "oim") {
            Map<Object, Object> map = readYaml(script, domainYaml)

            for (Object key : map.keySet()) {
                if (key.equals("spec")) {
                    LinkedHashMap specs = map.get("spec")

                    for (Object spec : specs.keySet()) {
                        if (spec.equals("clusters")) {
                            List clusters = specs.get("clusters")

                            for (LinkedHashMap cluster : clusters) {
                                cluster.put("clusterName", "soa_cluster")
                            }
                        }
                    }
                }
            }

            writeYaml(script, map, domainYaml)

            for (Object key : map.keySet()) {
                if (key.equals("spec")) {
                    println "Found spec"
                    LinkedHashMap specs = map.get("spec")

                    for (Object spec : specs.keySet()) {
                        if (spec.equals("clusters")) {
                            println "Found clusters"
                            List clusters = specs.get("clusters")

                            LinkedHashMap cluster = new LinkedHashMap()
                            cluster.put("clusterName", "oim_cluster")
                            cluster.put("serverStartState", "RUNNING")
                            cluster.put("replicas", 2)
                            clusters.add(cluster)
                        }
                    }
                }
            }

            writeYaml(script, map, domainYaml + productId)
        }
    }

    static readYaml(script, yamlFile) {
        def yamlFileContents = script.readFile yamlFile

        Yaml yamlReader = new Yaml()
        Map<Object, Object> map = yamlReader.load(yamlFileContents)
        return map
    }

    static readYaml(yamlFile) {
        InputStream ios = new FileInputStream(new File(yamlFile))

        Yaml yamlReader = new Yaml()
        Map<Object, Object> map = yamlReader.load(ios)
        return map
    }

    static writeYaml(script, map, yamlFile) {
        script.writeFile file: yamlFile + ".yaml", text: getYamlContent(map)
//        script.writeYaml file: yamlFile + ".yaml", data: map
    }

    @NonCPS
    static writeYaml(map, yamlFile) {
        DumperOptions options = new DumperOptions()
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
        options.setPrettyFlow(true)
        options.setIndent(2)
        options.setExplicitStart(true)

        FileWriter yamlFileContents = new FileWriter(yamlFile)
        Yaml yamlWriter = new Yaml(options)
        yamlWriter.dump(map, yamlFileContents)
    }

    @NonCPS
    static String getYamlContent(map) {
        DumperOptions options = new DumperOptions()
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
        options.setPrettyFlow(true)
        options.setIndent(2)
        options.setExplicitStart(true)

        Yaml yamlWriter = new Yaml(options)
        String yamlFileContents = yamlWriter.dump(map)

        return yamlFileContents
    }

    static printMap(map) {
        map.each { k, v -> println "${k}:${v}" }
    }
}
