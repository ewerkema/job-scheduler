# -*- coding: utf-8 -*-
# Generated by Django 1.10.6 on 2017-04-02 13:03
from __future__ import unicode_literals

from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    dependencies = [
        ('scheduler_web', '0001_initial'),
    ]

    operations = [
        migrations.AddField(
            model_name='jobschedulingevent',
            name='server',
            field=models.ForeignKey(blank=True, null=True, on_delete=django.db.models.deletion.SET_NULL, to='scheduler_web.Server'),
        ),
    ]
