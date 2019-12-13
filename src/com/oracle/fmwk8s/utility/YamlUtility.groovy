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
    static domainYaml

    static void main(String[] args) {
        YamlUtility yamlUtility = new YamlUtility();
        yamlUtility.generateDomainYaml("oim", "domain.yaml")
    }

    static generatePeristentVolumeInputsYaml(script, domainName, domainNamespace, nfsDomainPath, pvInputsYamlFile) {
        Map<Object, Object> map = readYaml(script, pvInputsYamlFile)

        map.put("baseName", domainNamespace.toString())
        map.put("domainUID", domainName.toString())
        map.put("namespace", domainNamespace.toString())
        map.put("weblogicDomainStorageType", "HOST_PATH")
        map.put("weblogicDomainStoragePath", nfsDomainPath.toString())
        map.put("weblogicDomainStorageReclaimPolicy", "Recycle")

        this.pvInputsMap = map
        writeYaml(script, map, pvInputsYamlFile)
    }

    static generateDomainInputsYaml(script, domainType, domainName, domainNamespace, domainInputsYamlFile) {
        Map<Object, Object> map = readYaml(script, domainInputsYamlFile)

//        def managedServerNameBase = map.get("managedServerNameBase").toString().replaceAll("_", "-")
        def clusterName = map.get("clusterName").toString().replaceAll("_", "-")

        map.put("adminPort", 17001)
        map.put("adminServerName", "admin-server")
        map.put("domainUID", domainName.toString())
        map.put("clusterName", clusterName.toString())
//        map.put("managedServerNameBase", managedServerNameBase.toString())
        if (domainType != null && !domainType.toString().equalsIgnoreCase("N/A")) {
            map.put("domainType", domainType.toString())
            if (domainType.toString().equalsIgnoreCase("osb")) {
                map.put("clusterName", "osb-cluster")
            }
        }
        map.put("domainHome", "/shared/domains/" + domainName.toString())
        map.put("configuredManagedServerCount", 4)
        map.put("initialManagedServerReplicas", 1)
        map.put("managedServerPort", 18001)
        map.put("image", Common.productImage.toString())
        map.put("imagePullSecretName", Common.registrySecret.toString())
        map.put("weblogicCredentialsSecretName", domainName.toString() + "-weblogic-credentials")
        map.put("logHome", "/shared/logs/" + domainName.toString())
        map.put("t3ChannelPort", 31012)
//        https://bug.oraclecorp.com/pls/bug/webbug_edit.edit_info_top?rptno=30176526
        map.put("exposeAdminT3Channel", false)
        map.put("t3PublicAddress", "fmwk8s.us.oracle.com")
        map.put("namespace", domainNamespace.toString())
        map.put("persistentVolumeClaimName", domainName.toString() + "-" + domainNamespace.toString() + "-pvc")
        map.put("domainPVMountPath", "/shared")
        map.put("rcuSchemaPrefix", domainName.toString())
        map.put("rcuDatabaseURL", Database.dbName.toString() + "." + domainNamespace.toString() + ":" + Database.dbPort.toString() + "/" + Database.dbName.toString() + "pdb.us.oracle.com")
        map.put("rcuCredentialsSecret", domainName.toString() + "-rcu-credentials")

        this.domainInputsMap = map
        writeYaml(script, map, domainInputsYamlFile)
    }

    static generateDomainYaml(script, productId, domainYaml) {
        Map<Object, Object> map = readYaml(script, domainYaml)
        this.domainYaml = map

        if ("${productId}" == "oim") {
            for (Object key : map.keySet()) {
                if (key.equals("spec")) {
                    LinkedHashMap specs = map.get("spec")

                    for (Object spec : specs.keySet()) {
                        if (spec.equals("clusters")) {
                            List clusters = specs.get("clusters")

                            for (LinkedHashMap cluster : clusters) {
                                cluster.put("clusterName", "soa_cluster")
                                cluster.put("replicas", 1)
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
                            cluster.put("replicas", 1)
                            clusters.add(0, cluster)
                        }
                    }
                }
            }

            writeYaml(script, map, domainYaml + productId)
        } else {
            writeYaml(script, map, domainYaml)
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
        doPrecreateServiceWA(map)
        script.writeFile file: yamlFile + ".yaml", text: getYamlContent(map)
//        script.writeYaml file: yamlFile + ".yaml", data: map
    }

    static doPrecreateServiceWA(map) {
        LinkedHashMap serverService = new LinkedHashMap()
        serverService.put("precreateService", true)

        for (Object key : map.keySet()) {
            if (key.equals("spec")) {
                LinkedHashMap specs = map.get("spec")

                for (Object spec : specs.keySet()) {
                    if (spec.equals("clusters")) {
                        List clusters = specs.get("clusters")

                        for (LinkedHashMap cluster : clusters) {
                            cluster.putIfAbsent("serverService", serverService)
                        }
                    }
                }
            }
        }

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
