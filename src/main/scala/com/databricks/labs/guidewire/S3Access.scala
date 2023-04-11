package com.databricks.labs.guidewire

import com.amazonaws.services.s3.model.ListObjectsRequest
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

object S3Access {
  def build = new S3Access
}

class S3Access() extends Serializable {

  lazy val s3Client: AmazonS3 = build()
  private val logger = LoggerFactory.getLogger(this.getClass)

  def build(): AmazonS3 = {
    AmazonS3ClientBuilder.standard().withForceGlobalBucketAccessEnabled(true).build()
  }

  def listTimestampDirectories(bucketName: String, bucketKey: String): Seq[Long] = {
    logger.info(s"Listing directories in [$bucketKey]")
    val listObjectsRequest = new ListObjectsRequest(bucketName, bucketKey, null, "/", Int.MaxValue)
    val response = s3Client.listObjects(listObjectsRequest)
    response.getCommonPrefixes.asScala.map(_.split("/").last.toLong)
  }

  def listParquetFiles(bucketName: String, bucketKey: String): Array[GwFile] = {
    logger.info(s"Listing parquet files in [$bucketKey]")
    val listObjectsRequest = new ListObjectsRequest(bucketName, bucketKey, null, "/", Int.MaxValue)
    val response = s3Client.listObjects(listObjectsRequest)
    response.getObjectSummaries.asScala.filter(file => {
      def accept(fileName: String): Boolean = {
        !fileName.startsWith(".") && fileName.contains(".parquet")
      }

      accept(file.getKey.split("/").last)
    }).map(summary => {
      GwFile(
        s"s3://${summary.getBucketName}/${summary.getKey}",
        summary.getSize,
        summary.getLastModified.getTime
      )
    }).sortBy(_.modificationTime).toArray
  }

  def readString(bucketName: String, bucketKey: String): String = {
    logger.info(s"Reading text from [$bucketKey]")
    s3Client.getObjectAsString(bucketName, bucketKey)
  }

  def readByteArray(bucketName: String, bucketKey: String): Array[Byte] = {
    logger.info(s"Reading byte array from [$bucketKey]")
    IOUtils.toByteArray(s3Client.getObject(bucketName, bucketKey).getObjectContent)
  }

}
