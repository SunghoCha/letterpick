ALTER TABLE newsletter_issue
    ADD FULLTEXT INDEX ft_newsletter_issue_subject_content_ngram (subject, content)
        WITH PARSER ngram;
