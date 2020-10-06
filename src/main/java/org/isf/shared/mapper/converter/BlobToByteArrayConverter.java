package org.isf.shared.mapper.converter;

import java.sql.Blob;
import java.sql.SQLException;

import org.modelmapper.AbstractConverter;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * @author akashytsa
 *
 */
@Slf4j
@Configuration
public class BlobToByteArrayConverter extends AbstractConverter<Blob, byte[]> {

	@Override
	protected byte[] convert(Blob data){
        byte[] blobAsBytes = new byte[0];
        try {
            if(data != null) {
                int blobLength = (int) data.length();
                blobAsBytes = data.getBytes(1, blobLength);
                data.free();
            }
        } catch (SQLException e) {
            log.error("", e);
        }
        return blobAsBytes;
	}

}
