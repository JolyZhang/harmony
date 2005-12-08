/* Copyright 1998, 2005 The Apache Software Foundation or its licensors, as applicable
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.net;


import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.util.Hashtable;
import java.util.StringTokenizer;

import com.ibm.oti.util.PriviAction;

/**
 * An instance of class URL specifies the location of a resource on the world
 * wide web as specified by RFC 1738.
 * 
 */

public final class URL implements java.io.Serializable {
	static final long serialVersionUID = -7627629688361524110L;

	private static final NetPermission specifyStreamHandlerPermission = new NetPermission(
			"specifyStreamHandler");

	private int hashCode;

	/**
	 * The receiver's filename.
	 * 
	 * @serial the file of this URL
	 * 
	 */
	private String file;

	/**
	 * The receiver's protocol identifier.
	 * 
	 * @serial the protocol of this URL (http, file)
	 * 
	 */
	private String protocol = null;

	/**
	 * The receiver's host name.
	 * 
	 * @serial the host of this URL
	 * 
	 */
	private String host;

	/**
	 * The receiver's port number.
	 * 
	 * @serial the port of this URL
	 * 
	 */
	private int port = -1;

	/**
	 * The receiver's authority.
	 * 
	 * @serial the authority of this URL
	 * 
	 */
	private String authority = null;

	/**
	 * The receiver's userInfo.
	 */
	private transient String userInfo = null;

	/**
	 * The receiver's path.
	 */
	private transient String path = null;

	/**
	 * The receiver's query.
	 */
	private transient String query = null;

	/**
	 * The receiver's reference.
	 * 
	 * @serial the reference of this URL
	 * 
	 */
	private String ref = null;

	/**
	 * Cache for storing protocol handler
	 */
	private static Hashtable streamHandlers = new Hashtable();

	/**
	 * The URL Stream (protocol) Handler
	 */
	transient URLStreamHandler strmHandler;

	/**
	 * The factory responsible for producing URL Stream (protocol) Handler
	 */
	private static URLStreamHandlerFactory streamHandlerFactory;

	/**
	 * Sets the URL Stream (protocol) handler factory. This method can be
	 * invoked only once during an application's lifetime.
	 * <p>
	 * A security check is performed to verify that the current Policy allows
	 * the stream handler factory to be set.
	 * 
	 * @param streamFactory
	 *            URLStreamHandlerFactory The factory to use for finding stream
	 *            handlers.
	 */
	public static synchronized void setURLStreamHandlerFactory(
			URLStreamHandlerFactory streamFactory) {
		if (streamHandlerFactory != null)
			throw new Error(com.ibm.oti.util.Msg.getString("K004b"));
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkSetFactory();
		streamHandlers.clear();
		streamHandlerFactory = streamFactory;
	}

	/**
	 * Constructs a new URL instance by parsing the specification.
	 * 
	 * @param spec
	 *            java.lang.String a URL specification.
	 * 
	 * @throws MalformedURLException
	 *             if the spec could not be parsed as an URL.
	 */
	public URL(String spec) throws MalformedURLException {
		this((URL) null, spec, (URLStreamHandler) null);
	}

	/**
	 * Constructs a new URL by parsing the specification given by
	 * <code>spec</code> and using the context provided by
	 * <code>context</code>.
	 * <p>
	 * The protocol of the specification is obtained by parsing the
	 * <code> spec </code> string.
	 * <p>
	 * If the <code>spec</code> does not specify a protocol:
	 * <ul>
	 * <li>If the context is <code>null</code>, then a
	 * <code>MalformedURLException</code>.</li>
	 * <li>If the context is not <code>null</code>, then the protocol is
	 * obtained from the context.</li>
	 * </ul>
	 * If the <code>spec</code> does specify a protocol:
	 * <ul>
	 * <li>If the context is <code>null</code>, or specifies a different
	 * protocol than the spec, the context is ignored.</li>
	 * <li>If the context is not <code>null</code> and specifies the same
	 * protocol as the specification, the properties of the new <code>URL</code>
	 * are obtained from the context.</li>
	 * </ul>
	 * 
	 * @param context
	 *            java.net.URL URL to use as context.
	 * @param spec
	 *            java.lang.String a URL specification.
	 * 
	 * @throws MalformedURLException
	 *             if the spec could not be parsed as an URL.
	 */
	public URL(URL context, String spec) throws MalformedURLException {
		this(context, spec, (URLStreamHandler) null);
	}

	/**
	 * Constructs a new URL by parsing the specification given by
	 * <code>spec</code> and using the context provided by
	 * <code>context</code>.
	 * <p>
	 * If the handler argument is non-null, a security check is made to verify
	 * that user-defined protocol handlers can be specified.
	 * <p>
	 * The protocol of the specification is obtained by parsing the
	 * <code> spec </code> string.
	 * <p>
	 * If the <code>spec</code> does not specify a protocol:
	 * <ul>
	 * <li>If the context is <code>null</code>, then a
	 * <code>MalformedURLException</code>.</li>
	 * <li>If the context is not <code>null</code>, then the protocol is
	 * obtained from the context.</li>
	 * </ul>
	 * If the <code>spec</code> does specify a protocol:
	 * <ul>
	 * <li>If the context is <code>null</code>, or specifies a different
	 * protocol than the spec, the context is ignored.</li>
	 * <li>If the context is not <code>null</code> and specifies the same
	 * protocol as the specification, the properties of the new <code>URL</code>
	 * are obtained from the context.</li>
	 * </ul>
	 * 
	 * @param context
	 *            java.net.URL URL to use as context.
	 * @param spec
	 *            java.lang.String a URL specification.
	 * @param handler
	 *            java.net.URLStreamHandler a URLStreamHandler.
	 * 
	 * @throws MalformedURLException
	 *             if the spec could not be parsed as an URL
	 */
	public URL(URL context, String spec, URLStreamHandler handler)
			throws MalformedURLException {
		if (handler != null) {
			SecurityManager sm = System.getSecurityManager();
			if (sm != null)
				sm.checkPermission(specifyStreamHandlerPermission);
			strmHandler = handler;
		}

		if (spec == null) {
			throw new MalformedURLException();
		}
		spec = spec.trim();

		// The spec includes a protocol if it includes a colon character
		// before the first occurance of a slash character. Note that,
		// "protocol" is the field which holds this URLs protocol.
		int index;
		try {
			index = spec.indexOf(':');
		} catch (NullPointerException e) {
			throw new MalformedURLException(e.toString());
		}
		int startIPv6Addr = spec.indexOf('[');
		if (index >= 0) {
			if ((startIPv6Addr == -1) || (index < startIPv6Addr)) {
				protocol = spec.substring(0, index);
				if (protocol.indexOf('/') >= 0) {
					protocol = null;
					index = -1;
				} else
					// Ignore case in protocol names.
					protocol = protocol.toLowerCase();
			}
		}

		if (protocol != null) {
			// If the context was specified, and it had the same protocol
			// as the spec, then fill in the receiver's slots from the values
			// in the context but still allow them to be over-ridden later
			// by the values in the spec.
			if (context != null && protocol.equals(context.getProtocol())) {
				String cPath = context.getPath();
				if (cPath != null && cPath.startsWith("/")) {
					set(protocol, context.getHost(), context.getPort(), context
							.getAuthority(), context.getUserInfo(), cPath,
							context.getQuery(), null);
				}
				if (strmHandler == null)
					strmHandler = context.strmHandler;
			}
		} else {
			// If the spec did not include a protocol, then the context
			// *must* be specified. Fill in the receiver's slots from the
			// values in the context, but still allow them to be over-ridden
			// by the values in the ("relative") spec.
			if (context == null)
				throw new MalformedURLException(com.ibm.oti.util.Msg.getString(
						"K00d8", spec));
			set(context.getProtocol(), context.getHost(), context.getPort(),
					context.getAuthority(), context.getUserInfo(), context
							.getPath(), context.getQuery(), null);
			if (strmHandler == null)
				strmHandler = context.strmHandler;
		}

		// If the stream handler has not been determined, set it
		// to the default for the specified protocol.
		if (strmHandler == null) {
			setupStreamHandler();
			if (strmHandler == null)
				throw new MalformedURLException(com.ibm.oti.util.Msg.getString(
						"K00b3", protocol));
		}

		// Let the handler parse the URL. If the handler throws
		// any exception, throw MalformedURLException instead.
		//
		// Note: We want "index" to be the index of the start of the scheme
		// specific part of the URL. At this point, it will be either
		// -1 or the index of the colon after the protocol, so we
		// increment it to point at either character 0 or the character
		// after the colon.
		try {
			strmHandler.parseURL(this, spec, ++index, spec.length());
		} catch (Exception e) {
			throw new MalformedURLException(e.toString());
		}

		if (port < -1 || port > 65535)
			throw new MalformedURLException(com.ibm.oti.util.Msg.getString(
					"K0325", port)); //$NON-NLS-1$
	}

	/**
	 * Constructs a new URL instance using the arguments provided.
	 * 
	 * @param protocol
	 *            String the protocol for the URL.
	 * @param host
	 *            String the name of the host.
	 * @param file
	 *            the name of the resource.
	 * 
	 * @throws MalformedURLException
	 *             if the parameters do not represent a valid URL.
	 */
	public URL(String protocol, String host, String file)
			throws MalformedURLException {
		this(protocol, host, -1, file, (URLStreamHandler) null);
	}

	/**
	 * Constructs a new URL instance using the arguments provided.
	 * 
	 * @param protocol
	 *            String the protocol for the URL.
	 * @param host
	 *            String the name of the host.
	 * @param port
	 *            int the port number.
	 * @param file
	 *            String the name of the resource.
	 * 
	 * @throws MalformedURLException
	 *             if the parameters do not represent a valid URL.
	 */
	public URL(String protocol, String host, int port, String file)
			throws MalformedURLException {
		this(protocol, host, port, file, (URLStreamHandler) null);
	}

	/**
	 * Constructs a new URL instance using the arguments provided.
	 * <p>
	 * If the handler argument is non-null, a security check is made to verify
	 * that user-defined protocol handlers can be specified.
	 * 
	 * @param protocol
	 *            the protocol for the URL.
	 * @param host
	 *            the name of the host.
	 * @param port
	 *            the port number.
	 * @param file
	 *            the name of the resource.
	 * @param handler
	 *            the stream handler that this URL uses.
	 * 
	 * @throws MalformedURLException
	 *             if the parameters do not represent an URL.
	 */
	public URL(String protocol, String host, int port, String file,
			URLStreamHandler handler) throws MalformedURLException {
		if (port < -1 || port > 65535)
			throw new MalformedURLException(com.ibm.oti.util.Msg.getString(
					"K0325", port)); //$NON-NLS-1$

		// Set the fields from the arguments. Handle the case where the
		// passed in "file" includes both a file and a ref part.
		int index = -1;
		index = file.indexOf("#", file.lastIndexOf("/"));
		if (index >= 0)
			set(protocol, host, port, file.substring(0, index), file
					.substring(index + 1));
		else
			set(protocol, host, port, file, null);

		// Set the stream handler for the URL either to the handler
		// argument if it was specified, or to the default for the
		// receiver's protocol if the handler was null.
		if (handler == null) {
			setupStreamHandler();
			if (strmHandler == null)
				throw new MalformedURLException(com.ibm.oti.util.Msg.getString(
						"K00b3", protocol));
		} else {
			SecurityManager sm = System.getSecurityManager();
			if (sm != null)
				sm.checkPermission(specifyStreamHandlerPermission);
			strmHandler = handler;
		}
	}

	void fixURL() {
		int index;
		if (host != null && host.length() > 0) {
			authority = host;
			if (port != -1)
				authority = authority + ":" + port;
		}
		if (host != null && (index = host.lastIndexOf('@')) > -1) {
			userInfo = host.substring(0, index);
			host = host.substring(index + 1);
		} else {
			userInfo = null;
		}
		if (file != null && (index = file.indexOf('?')) > -1) {
			query = file.substring(index + 1);
			path = file.substring(0, index);
		} else {
			query = null;
			path = file;
		}
	}

	/**
	 * Sets the properties of this URL using the provided arguments. This method
	 * is used both within this class and by the <code>URLStreamHandler</code>
	 * code.
	 * 
	 * @param protocol
	 *            the new protocol.
	 * @param host
	 *            the new host name.
	 * @param port
	 *            the new port number.
	 * @param file
	 *            the new file component.
	 * @param ref
	 *            the new reference.
	 * 
	 * @see URL
	 * @see URLStreamHandler
	 */
	protected void set(String protocol, String host, int port, String file,
			String ref) {
		if (this.protocol == null) // The protocol cannot be changed
			this.protocol = protocol;
		this.host = host;
		this.file = file;
		this.port = port;
		this.ref = ref;
		hashCode = 0;
		fixURL();
	}

	/**
	 * Compares the argument to the receiver, and answers true if they represent
	 * the same URL. Two URLs are equal if they have the same file, host, port,
	 * protocol, and ref components.
	 * 
	 * @param o
	 *            the object to compare with this URL.
	 * @return <code>true</code> if the object is the same as this URL,
	 *         <code>false</code> otherwise.
	 * 
	 * @see #hashCode
	 */
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (this == o)
			return true;
		if (this.getClass() != o.getClass())
			return false;
		return strmHandler.equals(this, (URL) o);
	}

	/**
	 * Answers true if the receiver and the argument refer to the same file. All
	 * components except the reference are compared.
	 * 
	 * @param otherURL
	 *            URL to compare against.
	 * @return true if the same resource, false otherwise
	 */
	public boolean sameFile(URL otherURL) {
		return strmHandler.sameFile(this, otherURL);
	}

	/**
	 * Answers a hash code for this URL object.
	 * 
	 * @return the hashcode for hashtable indexing
	 */
	public int hashCode() {
		if (hashCode == 0)
			hashCode = strmHandler.hashCode(this);
		return hashCode;
	}

	/**
	 * Sets the receiver's stream handler to one which is appropriate for its
	 * protocol. Throws a MalformedURLException if no reasonable handler is
	 * available.
	 * <p>
	 * Note that this will overwrite any existing stream handler with the new
	 * one. Senders must check if the strmHandler is null before calling the
	 * method if they do not want this behavior (a speed optimization).
	 */

	void setupStreamHandler() {
		// Check for a cached (previously looked up) handler for
		// the requested protocol.
		strmHandler = (URLStreamHandler) streamHandlers.get(protocol);
		if (strmHandler != null)
			return;

		// If there is a stream handler factory, then attempt to
		// use it to create the handler.
		if (streamHandlerFactory != null) {
			strmHandler = streamHandlerFactory.createURLStreamHandler(protocol);
			if (strmHandler != null) {
				streamHandlers.put(protocol, strmHandler);
				return;
			}
		}

		// Check if there is a list of packages which can provide handlers.
		// If so, then walk this list looking for an applicable one.
		String packageList = (String) AccessController
				.doPrivileged(new PriviAction("java.protocol.handler.pkgs"));
		if (packageList != null) {
			StringTokenizer st = new StringTokenizer(packageList, "|");
			while (st.hasMoreTokens()) {
				String className = st.nextToken() + "." + protocol + ".Handler";
				try {
					strmHandler = (URLStreamHandler) Class.forName(className,
							true, ClassLoader.getSystemClassLoader())
							.newInstance();
					streamHandlers.put(protocol, strmHandler);
					return;
				} catch (Exception e) {
				}
			}
		}

		// No one else has provided a handler, so try our internal one.
		try {
			String className = "com.ibm.oti.net.www.protocol." + protocol
					+ ".Handler";
			strmHandler = (URLStreamHandler) Class.forName(className)
					.newInstance();
			streamHandlers.put(protocol, strmHandler);
		} catch (Exception e) {
		}
	}

	/**
	 * Answers an Object representing the resource referenced by this URL.
	 * 
	 * @return The object of the resource pointed by this URL.
	 * 
	 * @throws IOException
	 *             If an error occured obtaining the content.
	 */
	public final Object getContent() throws IOException {
		return openConnection().getContent();
	}

	/**
	 * Answers an Object representing the resource referenced by this URL.
	 * 
	 * @param types
	 *            The list of acceptable content types
	 * @return The object of the resource pointed by this URL, or null if the
	 *         content does not match a specified content type.
	 * 
	 * @throws IOException
	 *             If an error occured obtaining the content.
	 */
	public final Object getContent(Class[] types) throws IOException {
		return openConnection().getContent(types);
	}

	/**
	 * Answers a stream for reading from this URL.
	 * 
	 * @return a stream on the contents of the resource.
	 * 
	 * @throws IOException
	 *             if a stream could not be created.
	 */
	public final InputStream openStream() throws java.io.IOException {
		return openConnection().getInputStream();
	}

	/**
	 * Creates a connection to this URL using the appropriate ProtocolHandler.
	 * 
	 * @return The connection to this URL.
	 * 
	 * @throws IOException
	 *             if the connection to the URL is not possible.
	 */
	public URLConnection openConnection() throws IOException {
		return strmHandler.openConnection(this);
	}

	/**
	 * Answers a string containing a concise, human-readable description of the
	 * receiver.
	 * 
	 * @return a printable representation for the receiver.
	 */
	public String toString() {
		return toExternalForm();
	}

	/**
	 * Create and return the String representation of this URL.
	 * 
	 * @return the external representation of this URL.
	 * 
	 * @see #toString()
	 * @see URL
	 * @see URLStreamHandler#toExternalForm(URL)
	 */
	public String toExternalForm() {
		if (strmHandler == null)
			return "unknown protocol(" + protocol + ")://" + host + file;
		return strmHandler.toExternalForm(this);
	}

	/**
	 * This method is called to restore the state of a URL object that has been
	 * serialized. The stream handler is determined from the URL's protocol.
	 * 
	 * @param stream
	 *            the stream to read from.
	 * 
	 * @throws IOException
	 *             if an IO Exception occurs while reading the stream or the
	 *             handler can not be found.
	 */
	private void readObject(java.io.ObjectInputStream stream)
			throws java.io.IOException {
		try {
			stream.defaultReadObject();
			if (host != null && authority == null)
				fixURL();
			else if (authority != null) {
				int index;
				if ((index = authority.lastIndexOf('@')) > -1)
					userInfo = authority.substring(0, index);
				if (file != null && (index = file.indexOf('?')) > -1) {
					query = file.substring(index + 1);
					path = file.substring(0, index);
				} else {
					path = file;
				}
			}
			setupStreamHandler();
			if (strmHandler == null)
				throw new IOException(com.ibm.oti.util.Msg.getString("K00b3",
						protocol));
		} catch (ClassNotFoundException e) {
			throw new java.io.IOException(e.toString());
		}
	}

	/**
	 * This method is called to write any non-transient, non-static variables
	 * into the output stream.
	 * <p>
	 * Note that, we really only need the readObject method but the spec that
	 * says readObject will be ignored if no writeObject is present.
	 * 
	 * @param s
	 *            the stream to write to.
	 * 
	 * @throws IOException
	 *             if an IO Exception occurs during the write.
	 */
	private void writeObject(java.io.ObjectOutputStream s)
			throws java.io.IOException {
		s.defaultWriteObject();
	}

	// Slot accessors follow...

	/**
	 * Answers the file component of this URL.
	 * 
	 * @return the receiver's file.
	 */
	public String getFile() {
		return file;
	}

	/**
	 * Answers the host component of this URL.
	 * 
	 * @return the receiver's host.
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Answers the port component of this URL.
	 * 
	 * @return the receiver's port.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Answers the protocol component of this URL.
	 * 
	 * @return the receiver's protocol.
	 */
	public String getProtocol() {
		return protocol;
	}

	/**
	 * Answers the reference component of this URL.
	 * 
	 * @return the receiver's reference component.
	 */
	public String getRef() {
		return ref;
	}

	/**
	 * Answers the query component of this URL.
	 * 
	 * @return the receiver's query.
	 */
	public String getQuery() {
		return query;
	}

	/**
	 * Answers the path component of this URL.
	 * 
	 * @return the receiver's path.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Answers the user info component of this URL.
	 * 
	 * @return the receiver's user info.
	 */
	public String getUserInfo() {
		return userInfo;
	}

	/**
	 * Answers the authority component of this URL.
	 * 
	 * @return the receiver's authority.
	 */
	public String getAuthority() {
		return authority;
	}

	/**
	 * Sets the properties of this URL using the provided arguments. This method
	 * is used both within this class and by the <code>URLStreamHandler</code>
	 * code.
	 * 
	 * @param protocol
	 *            the new protocol.
	 * @param host
	 *            the new host name.
	 * @param port
	 *            the new port number.
	 * @param authority
	 *            the new authority.
	 * @param userInfo
	 *            the new user info.
	 * @param path
	 *            the new path component.
	 * @param query
	 *            the new query.
	 * @param ref
	 *            the new reference.
	 * 
	 * @see URL
	 * @see URLStreamHandler
	 */
	protected void set(String protocol, String host, int port,
			String authority, String userInfo, String path, String query,
			String ref) {
		String file = path;
		if (query != null && !query.equals("")) {
			if (file != null)
				file = file + "?" + query;
			else
				file = "?" + query;
		}
		set(protocol, host, port, file, ref);
		this.authority = authority;
		this.userInfo = userInfo;
		this.path = path;
		this.query = query;
	}

	URLStreamHandler getStreamHandler() {
		return strmHandler;
	}

	/**
	 * Returns the default port for this URL as
	 * defined by the URLStreamHandler.
	 *
	 * @return the default port for this URL
	 *
	 * @see 		URLStreamHandler#getDefaultPort
	 */
	public int getDefaultPort() {
		return strmHandler.getDefaultPort();
	}
}