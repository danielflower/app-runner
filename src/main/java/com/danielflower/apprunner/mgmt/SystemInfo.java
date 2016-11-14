package com.danielflower.apprunner.mgmt;

import com.jcraft.jsch.Identity;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;
import com.jcraft.jsch.Session;
import org.apache.commons.collections4.list.UnmodifiableList;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.io.output.WriterOutputStream;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SystemInfo {
    private static final Logger log = LoggerFactory.getLogger(SystemInfo.class);

    public final String hostName;
    public final String user;
    public final Long pid;
    public final List<String> publicKeys;
    public final String osName;
    public final int numCpus;
    private final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

    public SystemInfo(String hostName, String user, Long pid, List<String> publicKeys, String osName, int numCpus) {
        this.hostName = hostName;
        this.user = user;
        this.pid = pid;
        this.publicKeys = new UnmodifiableList<>(publicKeys);
        this.osName = osName;
        this.numCpus = numCpus;
    }



    private static List<String> getPublicKeys() throws Exception {
        return new JschConfigSessionFactory() {
            @Override
            protected void configure(OpenSshConfig.Host hc, Session session) {
            }
            List<String> getPublicKeys() throws Exception {
                JSch jSch = createDefaultJSch(FS.DETECTED);
                List<String> keys = new ArrayList<>();
                for (Object o : jSch.getIdentityRepository().getIdentities()) {
                    Identity i = (Identity) o;
                    KeyPair keyPair = KeyPair.load(jSch, i.getName(), null);
                    StringBuilder sb = new StringBuilder();
                    try (StringBuilderWriter sbw = new StringBuilderWriter(sb);
                         OutputStream os = new WriterOutputStream(sbw)) {
                        keyPair.writePublicKey(os, keyPair.getPublicKeyComment());
                    } finally {
                        keyPair.dispose();
                    }
                    keys.add(sb.toString().trim());
                }
                return keys;
            }
        }.getPublicKeys();
    }


    public static SystemInfo create() {
        Runtime runtime = Runtime.getRuntime();
        String name = ManagementFactory.getRuntimeMXBean().getName();
        Long pid = Pattern.matches("[0-9]+@.*", name) ? Long.parseLong(name.substring(0, name.indexOf('@'))) : null;
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            log.warn("Could not find host name so it will not be exposed on the System REST API", e);
            host = null;
        }

        List<String> publicKeys;
        try {
            publicKeys = getPublicKeys();
        } catch (Exception e) {
            log.warn("Could not detect public keys so they will not be exposed on the System REST API.", e);
            publicKeys = new ArrayList<>();
        }
        return new SystemInfo(host, System.getProperty("user.name"), pid, publicKeys, System.getProperty("os.name"), runtime.availableProcessors());
    }

    public long uptimeInMillis() {
        return runtimeMXBean.getUptime();
    }

    @Override
    public String toString() {
        return "SystemInfo{" +
            "hostName='" + hostName + '\'' +
            ", user='" + user + '\'' +
            ", pid=" + pid +
            ", publicKeys=" + publicKeys +
            ", osName='" + osName + '\'' +
            ", numCpus=" + numCpus +
            '}';
    }
}
