/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: AccessAccept.java,v 1.2 2008/06/25 05:42:00 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 * Portions Copyrighted [2015] [Intellectual Reserve, Inc (IRI)]
 */
package org.forgerock.openam.radius.common.packet;

import org.forgerock.openam.radius.common.Attribute;
import org.forgerock.openam.radius.common.AttributeType;
import org.forgerock.openam.radius.common.OctetUtils;

/**
 * Class representing the structure of the CHAP-Challenge attribute as specified in section 5.40 of RFC 2865 and able to
 * be instantiated from the on-the-wire bytes. This attribute is only included in access-requests.
 */
public class CHAPChallengeAttribute extends Attribute {
    /**
     * The CHAP Challenge sent by the NAS to a PPP Challenge-Handshake Authentication Protocol (CHAP) user.
     */
    private String str = null;

    /**
     * Constructs a new instance from the on-the-wire bytes for this attribute including the prefixing attribute-type
     * code octet and length octet.
     *
     * @param octets the on-the-wire bytes from which to construct this instance
     */
    public CHAPChallengeAttribute(byte[] octets) {
        super(octets);
        str = new String(octets, 2, octets.length - 2);
    }

    /**
     * Constructs a new instance from the model object it will represent.
     *
     * @param challenge The CHAP challenge value.
     */
    public CHAPChallengeAttribute(String challenge) {
        super(OctetUtils.toOctets(AttributeType.CHAP_CHALLENGE, challenge));
        str = new String(super.getOctets(), 2, super.getOctets().length - 2); // ensures strings too long get trimmed
    }

    /**
     * Returns the value of the attribute.
     *
     * @return the value.
     */
    public String getString() {
        return str;
    }

    /**
     * Used by super class to log the attribute's contents when packet logging is enabled.
     *
     * @return content representation for traffic logging
     */
    public String toStringImpl() {
        return str;
    }

}
